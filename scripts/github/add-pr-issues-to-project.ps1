param(
  [string]$Org = "hmislk",
  [int]$ProjectNumber = 11,
  [string]$ProjectName,
  [string]$Repo,               # e.g., "hmis-web"
  [int[]]$PrNumbers,           # one or more PR numbers
  [string]$Author,             # alternative to PR numbers: scan PRs by author within org
  [string]$Token = $env:GITHUB_TOKEN,
  [switch]$DryRun              # show actions without mutating
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "[ERR ] $msg" -ForegroundColor Red }

if (-not $Token) {
  Write-Err "GitHub token is required. Pass -Token or set GITHUB_TOKEN."
  exit 1
}

if ((-not $Repo -or -not $PrNumbers) -and (-not $Author)) {
  Write-Err "Provide either -Repo and -PrNumbers, or -Author to scan across org."
  exit 1
}

$baseHeaders = @{
  'Authorization' = "Bearer $Token"
  'User-Agent'    = 'hmis-automation-script'
}

function Invoke-GQL($query, $variables) {
  $body = @{ query = $query; variables = $variables } | ConvertTo-Json -Depth 10
  $resp = Invoke-RestMethod -Method Post -Uri 'https://api.github.com/graphql' -Headers $baseHeaders -ContentType 'application/json' -Body $body
  $props = $resp.PSObject.Properties.Name
  $hasErrors = $props -contains 'errors'
  if ($hasErrors -and $resp.errors) {
    throw ("GraphQL error: " + ($resp.errors | ConvertTo-Json -Depth 10))
  }
  if (-not ($props -contains 'data')) {
    throw "GraphQL response missing 'data' field."
  }
  return $resp.data
}

function Invoke-GHRest($method, $url, $bodyObj) {
  $headers = $baseHeaders.Clone()
  # Include preview for classic Projects API
  $headers['Accept'] = 'application/vnd.github+json, application/vnd.github.inertia-preview+json'
  if ($null -ne $bodyObj) {
    $body = ($bodyObj | ConvertTo-Json -Depth 10)
  } else {
    $body = $null
  }
  if ($DryRun) {
    Write-Info "DRY-RUN $method $url"; if ($body) { Write-Host $body }
    return $null
  }
  if ($method -in 'GET','DELETE') {
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers
  } else {
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType 'application/json' -Body $body
  }
}

function Get-ProjectContext {
  $q = @'
    query($org:String!, $number:Int!) {
      organization(login:$org) {
        projectV2(number:$number) {
          id
          title
          number
          closed
          fields(first: 100) {
            nodes {
              __typename
              ... on ProjectV2FieldCommon { id name }
              ... on ProjectV2SingleSelectField { id name options { id name } }
            }
          }
        }
        project(number:$number) {
          id
          number
          name
          closed
        }
      }
    }
'@
  $data = Invoke-GQL $q @{ org = $Org; number = $ProjectNumber }
  $orgNode = $data.organization
  if ($orgNode.projectV2) {
    $pv2 = $orgNode.projectV2
    Write-Info "Detected Projects v2: '$($pv2.title)' (#$($pv2.number))"
    # Find Status field and the target option
    $statusField = $pv2.fields.nodes | Where-Object { $_.__typename -eq 'ProjectV2SingleSelectField' -and $_.name -eq 'Status' } | Select-Object -First 1
    if (-not $statusField) { Write-Warn "Status field not found on project v2." }
    $targetOption = $null
    if ($statusField -and $statusField.options) {
      $targetOption = $statusField.options | Where-Object { $_.name -eq 'Reviewing & Merging' -or $_.name -eq 'Reviewing & Merging' } | Select-Object -First 1
      if (-not $targetOption) {
        # try case-insensitive match variations
        $targetOption = $statusField.options | Where-Object { $_.name -match 'review(ing)?' -and $_.name -match 'merge' } | Select-Object -First 1
      }
    }
    return @{ type = 'v2'; projectId = $pv2.id; statusFieldId = $statusField.id; statusOptionId = $targetOption.id; projectTitle = $pv2.title }
  }
  # Fallback to classic
  $projClassic = $orgNode.project
  if ($projClassic) {
    Write-Info "Detected Classic Project: '$($projClassic.name)' (#$($projClassic.number))"
    # Use REST to get columns (GraphQL classic columns support is limited)
    $projects = Invoke-GHRest GET "https://api.github.com/orgs/$Org/projects" $null
    $project = $projects | Where-Object { $_.number -eq $ProjectNumber } | Select-Object -First 1
    if (-not $project -and $ProjectName) {
      $project = $projects | Where-Object { $_.name -eq $ProjectName } | Select-Object -First 1
    }
    if (-not $project) { throw "Classic project #$ProjectNumber not found via REST." }
    $columns = Invoke-GHRest GET $project.columns_url $null
    $column = $columns | Where-Object { $_.name -eq 'Reviewing & Merging' } | Select-Object -First 1
    if (-not $column) {
      $colNames = ($columns | ForEach-Object { $_.name }) -join ', '
      throw "Column 'Reviewing & Merging' not found. Available: $colNames"
    }
    return @{ type = 'classic'; projectId = $project.id; columnId = $column.id; projectTitle = $project.name }
  }
  throw "Project #$ProjectNumber not found on org '$Org'"
}

function Get-ProjectV2ExistingIssueIds($org, $projectNumber) {
  $ids = @{}
  $after = $null
  $hasNext = $true
  while ($hasNext) {
    $q = @'
      query($org:String!, $number:Int!, $after:String) {
        organization(login:$org) {
          projectV2(number:$number) {
            items(first:100, after:$after) {
              pageInfo { hasNextPage endCursor }
              nodes {
                content {
                  __typename
                  ... on Issue { id number url repository { owner { login } name } }
                }
              }
            }
          }
        }
      }
'@
    $d = Invoke-GQL $q @{ org = $org; number = $projectNumber; after = $after }
    $items = $d.organization.projectV2.items
    foreach ($n in $items.nodes) {
      if ($n.content.__typename -eq 'Issue' -and $n.content.id) {
        $ids[$n.content.id] = $true
      }
    }
    $after = $items.pageInfo.endCursor
    $hasNext = $items.pageInfo.hasNextPage
  }
  return $ids
}

function Get-IssuesFromPR($owner, $repoName, $prNumber) {
  $q = @'
    query($owner:String!, $repo:String!, $number:Int!) {
      repository(owner:$owner, name:$repo) {
        pullRequest(number:$number) {
          number
          url
          closingIssuesReferences(first: 100) {
            nodes { id number url title repository { owner { login } name } }
          }
        }
      }
    }
'@
  $d = Invoke-GQL $q @{ owner = $owner; repo = $repoName; number = $prNumber }
  $pr = $d.repository.pullRequest
  if (-not $pr) { Write-Warn "PR not found: $owner/$repoName#$prNumber"; return @() }
  $nodes = $pr.closingIssuesReferences.nodes
  if (-not $nodes) { return @() }
  return $nodes | ForEach-Object {
    [pscustomobject]@{
      nodeId = $_.id
      number = $_.number
      url    = $_.url
      title  = $_.title
      owner  = $_.repository.owner.login
      repo   = $_.repository.name
      key    = "{0}/{1}#{2}" -f $_.repository.owner.login, $_.repository.name, $_.number
    }
  }
}

function Get-IssuesFromAuthor($org, $author) {
  $issues = @{}
  $after = $null
  do {
    $queryString = "org:$org is:pr author:$author"
    $q = @'
      query($q:String!, $after:String) {
        search(query:$q, type:ISSUE, first:50, after:$after) {
          pageInfo { hasNextPage endCursor }
          nodes {
            ... on PullRequest {
              number
              url
              repository { owner { login } name }
              closingIssuesReferences(first: 100) {
                nodes { id number url title repository { owner { login } name } }
              }
            }
          }
        }
      }
'@
    $d = Invoke-GQL $q @{ q = $queryString; after = $after }
    foreach ($node in $d.search.nodes) {
      $repoOwner = $node.repository.owner.login
      $repoName  = $node.repository.name
      foreach ($iss in $node.closingIssuesReferences.nodes) {
        $key = "{0}/{1}#{2}" -f $iss.repository.owner.login, $iss.repository.name, $iss.number
        if (-not $issues.ContainsKey($key)) {
          $issues[$key] = [pscustomobject]@{
            nodeId = $iss.id
            number = $iss.number
            url    = $iss.url
            title  = $iss.title
            owner  = $iss.repository.owner.login
            repo   = $iss.repository.name
            key    = $key
          }
        }
      }
    }
    $after = $d.search.pageInfo.endCursor
    $hasNext = $d.search.pageInfo.hasNextPage
  } while ($hasNext)
  return $issues.Values
}

function Add-Issue-To-ProjectV2($projCtx, $issueNodeId) {
  $addQ = @'
    mutation($projectId:ID!, $contentId:ID!) {
      addProjectV2ItemById(input:{ projectId:$projectId, contentId:$contentId }) {
        item { id }
      }
    }
'@
  try {
    if ($DryRun) { Write-Info "DRY-RUN add v2 item: $issueNodeId"; return $null }
    $addResp = Invoke-GQL $addQ @{ projectId = $projCtx.projectId; contentId = $issueNodeId }
    return $addResp.addProjectV2ItemById.item.id
  } catch {
    Write-Warn "Add to v2 project may have failed (possibly already exists): $($_.Exception.Message)"
    return $null
  }
}

function Set-V2-Status($projCtx, $itemId) {
  if (-not $projCtx.statusFieldId -or -not $projCtx.statusOptionId) {
    Write-Warn "Status field or option not found; skipping status set."
    return
  }
  $mut = @'
    mutation($projectId:ID!, $itemId:ID!, $fieldId:ID!, $optionId:String!) {
      updateProjectV2ItemFieldValue(input:{ projectId:$projectId, itemId:$itemId, fieldId:$fieldId, value:{ singleSelectOptionId:$optionId } }) {
        projectV2Item { id }
      }
    }
'@
  if ($DryRun) { Write-Info "DRY-RUN set v2 status: item=$itemId"; return }
  Invoke-GQL $mut @{ projectId = $projCtx.projectId; itemId = $itemId; fieldId = $projCtx.statusFieldId; optionId = $projCtx.statusOptionId } | Out-Null
}

function Add-Issue-To-ClassicColumn($columnId, $owner, $repoName, $issueNumber) {
  $issue = Invoke-GHRest GET "https://api.github.com/repos/$owner/$repoName/issues/$issueNumber" $null
  $issueId = $issue.id
  try {
    Invoke-GHRest POST "https://api.github.com/projects/columns/$columnId/cards" @{ content_id = $issueId; content_type = 'Issue' } | Out-Null
  } catch {
    Write-Warn "Failed to add classic card for $owner/$repoName#$issueNumber (may already exist): $($_.Exception.Message)"
  }
}

# Main
$proj = Get-ProjectContext
Write-Info "Target project: $($proj.projectTitle) [$($proj.type)]"

$existingV2Issues = $null
if ($proj.type -eq 'v2') {
  Write-Info "Fetching existing project items to skip duplicates"
  $existingV2Issues = Get-ProjectV2ExistingIssueIds -org $Org -projectNumber $ProjectNumber
  Write-Info ("Existing issues found in project: {0}" -f $existingV2Issues.Count)
}

$issueSet = @{}
if ($Repo -and $PrNumbers) {
  foreach ($pr in $PrNumbers) {
    Write-Info "Collecting issues from $Org/$Repo PR #$pr"
    foreach ($i in (Get-IssuesFromPR -owner $Org -repoName $Repo -prNumber $pr)) {
      if (-not $issueSet.ContainsKey($i.key)) { $issueSet[$i.key] = $i }
    }
  }
}
if ($Author) {
  Write-Info "Collecting issues from PRs authored by '$Author' in org '$Org'"
  foreach ($i in (Get-IssuesFromAuthor -org $Org -author $Author)) {
    if (-not $issueSet.ContainsKey($i.key)) { $issueSet[$i.key] = $i }
  }
}

if ($issueSet.Count -eq 0) {
  Write-Warn "No linked issues found. Nothing to add."
  exit 0
}

Write-Info "Found $($issueSet.Count) unique linked issue(s)."

foreach ($issue in $issueSet.Values) {
  if ($proj.type -eq 'v2') {
    if ($existingV2Issues -and $existingV2Issues.ContainsKey($issue.nodeId)) {
      Write-Info "Skipping existing: $($issue.key)"
      continue
    }
    Write-Info "Adding $($issue.key) - $($issue.title)"
    $itemId = Add-Issue-To-ProjectV2 -projCtx $proj -issueNodeId $issue.nodeId
    if ($itemId) { Set-V2-Status -projCtx $proj -itemId $itemId }
  } else {
    Write-Info "Adding $($issue.key) - $($issue.title)"
    Add-Issue-To-ClassicColumn -columnId $proj.columnId -owner $issue.owner -repoName $issue.repo -issueNumber $issue.number
  }
}

Write-Info "Done."
