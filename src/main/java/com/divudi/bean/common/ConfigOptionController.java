package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.OptionValueType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.ConfigOption;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.ConfigOptionFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import com.divudi.service.AuditService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.faces.application.FacesMessage;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class ConfigOptionController implements Serializable {

    @EJB
    private ConfigOptionFacade optionFacade;

    @Inject
    private SessionController sessionController;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    AuditService auditService;

    private ConfigOption option;
    private Institution institution;
    private Department department;
    private WebUser webUser;
    private List<ConfigOption> options;
    private List<ConfigOption> filteredOptions;
    private List<ConfigOption> selectedOptions = new ArrayList<>();
    private List<ConfigOptionDuplicateGroup> duplicateGroups;
    private UploadedFile uploadedFile;

    private String key;
    private String value;
    private String enumType;
    private String enumValue;
    private OptionValueType optionValueType;

    /**
     * Creates a new instance of OptionController
     */
    public ConfigOptionController() {
    }

    public boolean getBooleanValueByKey(String key) {
        return getBooleanValueByKey(key, true);
    }

    public boolean getBooleanValueByKey(String key, boolean defaultValue) {
        String departmentName;
        if (sessionController.getDepartment() != null) {
            departmentName = sessionController.getDepartment().getName();
        } else {
            return configOptionApplicationController.getBooleanValueByKey(key, defaultValue);
        }
        return configOptionApplicationController.getBooleanValueByKey(departmentName + " - " + key, defaultValue);
    }

    public void setBooleanValueByKey(String key, boolean value) {
        String departmentName;
        if (sessionController.getDepartment() != null) {
            departmentName = sessionController.getDepartment().getName();
            configOptionApplicationController.setBooleanValueByKey(departmentName + " - " + key, value);
        } else {
            configOptionApplicationController.setBooleanValueByKey(key, value);
        }
    }

    public String navigateToDepartmentOptions() {
        institution = null;
        department = sessionController.getDepartment();
        webUser = null;
        return "/admin/institutions/admin_mange_department_options?faces-redirect=true";
    }

    public String navigateToApplicationOptions() {
        institution = null;
        department = null;
        webUser = null;
        options = getApplicationOptions();
        return "/admin/institutions/admin_mange_application_options?faces-redirect=true";
    }

    public String navigateToInstitutionOptions() {
        institution = sessionController.getInstitution();
        department = sessionController.getDepartment();
        webUser = null;
        return "/admin/institutions/admin_mange_institution_options?faces-redirect=true";
    }

    public String navigateToUserOptions() {
        institution = sessionController.getInstitution();
        department = sessionController.getDepartment();
        webUser = null;
        return "/admin/institutions/admin_mange_user_options?faces-redirect=true";
    }

    public void deleteOption(ConfigOption delo) {
        if (delo == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return;
        }
        Map<String, Object> before = new HashMap<>();
        before.put("optionKey", delo.getOptionKey());
        before.put("optionValue", delo.getOptionValue());

        delo.setRetireComments("del");
        delo.setRetired(true);
        delo.setRetiredAt(new Date());
        delo.setRetirer(sessionController.getLoggedUser());

        saveOption(delo);

        Map<String, Object> after = new HashMap<>();
        after.put("optionKey", delo.getOptionKey());
        after.put("retired", true);

        auditService.logAudit(before, after, sessionController.getLoggedUser(), ConfigOption.class.getSimpleName(), "Delete Config Option");
        configOptionApplicationController.loadApplicationOptions();
        listApplicationOptions();
        JsfUtil.addSuccessMessage("Deleted");
    }

    public void bulkDeleteSelectedOptions() {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            JsfUtil.addErrorMessage("No options selected for deletion");
            return;
        }

        int deletedCount = 0;
        for (ConfigOption option : selectedOptions) {
            if (option != null) {
                Map<String, Object> before = new HashMap<>();
                before.put("optionKey", option.getOptionKey());
                before.put("optionValue", option.getOptionValue());

                option.setRetireComments("bulk del");
                option.setRetired(true);
                option.setRetiredAt(new Date());
                option.setRetirer(sessionController.getLoggedUser());

                saveOption(option);

                Map<String, Object> after = new HashMap<>();
                after.put("optionKey", option.getOptionKey());
                after.put("retired", true);

                auditService.logAudit(before, after, sessionController.getLoggedUser(), ConfigOption.class.getSimpleName(), "Bulk Delete Config Option");
                deletedCount++;
            }
        }

        selectedOptions.clear();
        configOptionApplicationController.loadApplicationOptions();
        listApplicationOptions();
        JsfUtil.addSuccessMessage("Deleted " + deletedCount + " options");
    }

    public StreamedContent exportSelectedOptions() {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            JsfUtil.addErrorMessage("No options selected for export");
            return null;
        }

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Option Key,Option Value,Value Type,Enum Type\n");

        for (ConfigOption option : selectedOptions) {
            if (option != null) {
                csvContent.append("\"").append(escapeQuotes(option.getOptionKey())).append("\",");
                csvContent.append("\"").append(escapeQuotes(option.getOptionValue())).append("\",");
                csvContent.append("\"").append(option.getValueType().toString()).append("\",");
                csvContent.append("\"").append(option.getEnumType() != null ? escapeQuotes(option.getEnumType()) : "").append("\"\n");
            }
        }

        InputStream stream = new ByteArrayInputStream(csvContent.toString().getBytes(StandardCharsets.UTF_8));
        return DefaultStreamedContent.builder()
                .name("config_options_export.csv")
                .contentType("text/csv")
                .stream(() -> stream)
                .build();
    }

    private String escapeQuotes(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    public String importOptionsFromCsv() {
        if (uploadedFile == null || uploadedFile.getSize() == 0) {
            JsfUtil.addErrorMessage("Please select a CSV file to import");
            return null;
        }

        try (InputStream in = uploadedFile.getInputStream()) {
            importOptionsFromFile(in);
            return "/admin/institutions/admin_mange_application_options?faces-redirect=true";
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error reading uploaded file: " + e.getMessage());
            return null;
        } finally {
            uploadedFile = null;
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile file = event.getFile();
            if (file != null) {
                importOptionsFromFile(file.getInputStream());
            }
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error reading uploaded file: " + e.getMessage());
        }
    }

    public void importOptionsFromFile(InputStream inputStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            String content = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");

            if (lines.length < 2) {
                JsfUtil.addErrorMessage("Invalid file format. Expected CSV with headers.");
                return;
            }

            int importedCount = 0;
            int updatedCount = 0;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = parseCsvLine(line);
                if (parts.length >= 3) {
                    String optionKey = parts[0];
                    String optionValue = parts[1];
                    String valueTypeStr = parts[2];
                    String enumType = parts.length > 3 ? parts[3] : null;

                    if (optionKey != null && !optionKey.trim().isEmpty()) {
                        OptionValueType valueType;
                        try {
                            valueType = OptionValueType.valueOf(valueTypeStr);
                        } catch (IllegalArgumentException e) {
                            valueType = OptionValueType.SHORT_TEXT;
                        }

                        ConfigOption existingOption = getOptionValueByKey(optionKey, OptionScope.APPLICATION, null, null, null);

                        if (existingOption != null) {
                            Map<String, Object> before = new HashMap<>();
                            before.put("optionKey", existingOption.getOptionKey());
                            before.put("optionValue", existingOption.getOptionValue());

                            existingOption.setOptionValue(optionValue);
                            existingOption.setValueType(valueType);
                            if (enumType != null && !enumType.trim().isEmpty()) {
                                existingOption.setEnumType(enumType);
                            }
                            saveOption(existingOption);

                            Map<String, Object> after = new HashMap<>();
                            after.put("optionKey", existingOption.getOptionKey());
                            after.put("optionValue", existingOption.getOptionValue());

                            auditService.logAudit(before, after, sessionController.getLoggedUser(), ConfigOption.class.getSimpleName(), "Import Update Config Option");
                            updatedCount++;
                        } else {
                            ConfigOption newOption = new ConfigOption();
                            newOption.setOptionKey(optionKey);
                            newOption.setOptionValue(optionValue);
                            newOption.setValueType(valueType);
                            newOption.setScope(OptionScope.APPLICATION);
                            if (enumType != null && !enumType.trim().isEmpty()) {
                                newOption.setEnumType(enumType);
                            }
                            newOption.setCreatedAt(new Date());
                            newOption.setCreater(sessionController.getLoggedUser());
                            optionFacade.create(newOption);

                            Map<String, Object> after = new HashMap<>();
                            after.put("optionKey", newOption.getOptionKey());
                            after.put("optionValue", newOption.getOptionValue());

                            auditService.logAudit(null, after, sessionController.getLoggedUser(), ConfigOption.class.getSimpleName(), "Import Create Config Option");
                            importedCount++;
                        }
                    }
                }
            }

            configOptionApplicationController.loadApplicationOptions();
            listApplicationOptions();

            String message = "Import completed. ";
            if (importedCount > 0) {
                message += importedCount + " new options created. ";
            }
            if (updatedCount > 0) {
                message += updatedCount + " options updated.";
            }
            JsfUtil.addSuccessMessage(message);

        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error processing file: " + e.getMessage());
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());

        return result.toArray(new String[0]);
    }

    public void saveDepartmentOption() {
        if (getKey() == null || getKey().isEmpty()) {
            JsfUtil.addErrorMessage("No ConfigOption");
            return;
        }
        if (getValue() == null || getValue().isEmpty()) {
            JsfUtil.addErrorMessage("No ConfigOption value");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("No Department");
            return;
        }
        String jpql = "";
        Map params = new HashMap();
        option = getFacade().findFirstByJpql(jpql, params);

        if (option == null) {
            option = new ConfigOption();
        } else {
            option.setOptionValue(value);

        }
        saveOption(option);
        JsfUtil.addSuccessMessage("Saved");
    }

    public void saveOption(ConfigOption option) {
        if (option == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        Map<String, Object> before = null;
        boolean creating = option.getId() == null;
        if (!creating) {
            ConfigOption existing = optionFacade.find(option.getId());
            if (existing != null) {
                before = new HashMap<>();
                before.put("optionKey", existing.getOptionKey());
                before.put("optionValue", existing.getOptionValue());
            }
        }

        if (creating) {
            option.setCreatedAt(new Date());
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.create(option);
        } else {
            optionFacade.edit(option);
        }
        configOptionApplicationController.loadApplicationOptions();

        Map<String, Object> after = new HashMap<>();
        after.put("optionKey", option.getOptionKey());
        after.put("optionValue", option.getOptionValue());

        String trigger = creating ? "Create Config Option" : "Update Config Option";
        auditService.logAudit(before, after, sessionController.getLoggedUser(), ConfigOption.class.getSimpleName(), trigger);
    }

    public ConfigOption getOptionValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.optionKey = :key AND o.scope = :scope AND COALESCE(o.retired, false) = false");
        Map<String, Object> params = new HashMap<>();
        params.put("key", key);
        params.put("scope", scope);
        switch (scope) {
            case DEPARTMENT:
                if (department != null) {
                    jpql.append(" AND o.department = :department");
                    params.put("department", department);
                } else {
                    // Optionally handle the null department case if needed
                }
                break;
            case INSTITUTION:
                if (institution != null) {
                    jpql.append(" AND o.institution = :institution");
                    params.put("institution", institution);
                } else {
                    // Optionally handle the null institution case if needed
                }
                break;
            case USER:
                if (webUser != null) {
                    jpql.append(" AND o.webUser = :webUser");
                    params.put("webUser", webUser);
                } else {
                    // Optionally handle the null webUser case if needed
                }
                break;
            case APPLICATION:
                // For APPLICATION scope, ensure that department, institution, and webUser are all null
                jpql.append(" AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL");
                break;
            default:
                throw new AssertionError("Unhandled scope: " + scope);
        }
        return optionFacade.findFirstByJpql(jpql.toString(), params);
    }

    public <E extends Enum<E>> E getEnumValue(ConfigOption option, Class<E> enumClass) {
        if (option.getEnumType() == null || option.getEnumValue() == null) {
            return null; // Or throw an exception if appropriate
        }
        if (!option.getEnumType().equals(enumClass.getName())) {
            throw new IllegalArgumentException("The option does not match the expected enum type.");
        }
        return E.valueOf(enumClass, option.getEnumValue());
    }

    public <E extends Enum<E>> E getEnumValueByKey(String key, Class<E> enumClass, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);

        if (option == null || option.getValueType() != OptionValueType.ENUM || !option.getEnumType().equals(enumClass.getName())) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.ENUM, "");
            option.setEnumType(enumClass.getName());
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }

        return getEnumValue(option, enumClass);
    }

    public Double getDoubleValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);
        if (option == null || option.getValueType() != OptionValueType.DOUBLE) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.DOUBLE, "0.0");
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }
        try {
            return Double.parseDouble(option.getOptionValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLongTextValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);
        if (option == null || option.getValueType() != OptionValueType.LONG_TEXT) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.LONG_TEXT, "");
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }
        return option.getOptionValue();
    }

    public String getShortTextValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);
        if (option == null || option.getValueType() != OptionValueType.SHORT_TEXT) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.SHORT_TEXT, "");
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }
        return option.getOptionValue();
    }

    public Long getLongValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);
        if (option == null || option.getValueType() != OptionValueType.LONG) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.LONG, "0");
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }

        try {
            // Attempt to convert the option's value to a Long
            return Long.parseLong(option.getOptionValue());
        } catch (NumberFormatException e) {
// Log or handle the case where the value cannot be parsed into a Long
            return null;
        }
    }

    public Boolean getBooleanValueByKey(String key, OptionScope scope, Institution institution, Department department, WebUser webUser) {
        ConfigOption option = getOptionValueByKey(key, scope, institution, department, webUser);
        if (option == null || option.getValueType() != OptionValueType.BOOLEAN) {
            option = optionFacade.createOptionIfNotExists(key, scope, institution, department, webUser, OptionValueType.BOOLEAN, "false");
            option.setCreater(sessionController.getLoggedUser());
            optionFacade.edit(option);
        }
        return Boolean.parseBoolean(option.getOptionValue());
    }

    public ConfigOption getOptionValueByKeyForInstitution(String key, Institution institution) {
        return getOptionValueByKey(key, OptionScope.INSTITUTION, institution, null, null);
    }

    public ConfigOption getOptionValueByKeyForDepartment(String key, Department department) {
        return getOptionValueByKey(key, OptionScope.DEPARTMENT, null, department, null);
    }

    public ConfigOption getOptionValueByKeyForWebUser(String key, WebUser webUser) {
        return getOptionValueByKey(key, OptionScope.USER, null, null, webUser);
    }

    public List<ConfigOption> searchOptions(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Splitting the searchText into keywords
        String[] keywords = searchText.split("\\s+");

        // Starting the JPQL query construction
        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.retired = false"); // Assuming there's a 'retired' field.

        // Parameters map for dynamic values
        Map<String, Object> params = new HashMap<>();

        // Constructing the dynamic part of the JPQL based on keywords
        for (int i = 0; i < keywords.length; i++) {
            String param = "keyword" + i;
            jpql.append(i == 0 ? " AND (" : " OR ");
            jpql.append("LOWER(o.optionKey) LIKE LOWER(:").append(param).append(")");
            params.put(param, "%" + keywords[i] + "%");
        }
        jpql.append(") ORDER BY o.optionKey"); // Closing the OR conditions and adding an ORDER BY clause

        // Utilizing the existing findByJpql method from AbstractFacade
        return getFacade().findByJpql(jpql.toString(), params);
    }

    public List<ConfigOption> getAllOptions(Object entity) {
        String jpql = "SELECT o FROM ConfigOption o WHERE COALESCE(o.retired, false) = false";
        Map<String, Object> params = new HashMap<>();

        if (entity == null) {
            jpql += " AND o.department IS NULL AND o.institution IS NULL AND o.webUser IS NULL";
        } else if (entity instanceof Department) {
            jpql += " AND o.department = :dept AND o.institution IS NULL AND o.webUser IS NULL";
            params.put("dept", (Department) entity);
        } else if (entity instanceof Institution) {
            jpql += " AND o.institution = :ins AND o.department IS NULL AND o.webUser IS NULL";
            params.put("ins", (Institution) entity);
        } else if (entity instanceof WebUser) {
            jpql += " AND o.webUser = :usr AND o.department IS NULL AND o.institution IS NULL";
            params.put("usr", (WebUser) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity type provided.");
        }

        jpql += " ORDER BY o.optionKey";

        return getFacade().findByJpql(jpql, params);
    }

    public List<ConfigOption> getDepartmentOptions(Department department) {
        return getAllOptions(department);
    }

    public List<ConfigOption> getInstitutionOptions(Institution institution) {
        return getAllOptions(institution);
    }

    public List<ConfigOption> getApplicationOptions() {
        return getAllOptions(null);
    }

    public List<ConfigOption> getWebUserOptions(WebUser webUser) {
        return getAllOptions(webUser);
    }

    public boolean filterByCustom(Object value, Object filter, Locale locale) {
        if (filter == null || filter.toString().isEmpty()) {
            return true;
        }
        String[] keywords = filter.toString().toLowerCase().split(" ");
        String optionKey = value.toString().toLowerCase();

        for (String keyword : keywords) {
            if (!optionKey.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private <T> T convertOptionValue(ConfigOption option, Class<T> type) {
        String value = option.getOptionValue();
        OptionValueType valueType = option.getValueType();

        try {
            switch (valueType) {
                case LONG_TEXT:
                case SHORT_TEXT:
                    if (String.class.equals(type)) {
                        return type.cast(value);
                    }
                    break; // Could add more specific handling for text types if needed
                case BOOLEAN:
                    if (Boolean.class.equals(type) || boolean.class.equals(type)) {
                        return type.cast(Boolean.valueOf(value));
                    }
                    break;
                case LONG:
                    if (Long.class.equals(type) || long.class.equals(type)) {
                        return type.cast(Long.valueOf(value));
                    }
                    break;
                case DOUBLE:
                    if (Double.class.equals(type) || double.class.equals(type)) {
                        return type.cast(Double.valueOf(value));
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to convert value to the requested type.", e);
        }

        throw new IllegalArgumentException("Unsupported type conversion requested: " + type.getSimpleName() + " for value type " + valueType);
    }

    public ConfigOptionFacade getOptionFacade() {
        return optionFacade;
    }

    public void setOptionFacade(ConfigOptionFacade optionFacade) {
        this.optionFacade = optionFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ConfigOption getOption() {
        return option;
    }

    public void setOption(ConfigOption option) {
        this.option = option;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public List<ConfigOption> getOptions() {
        return options;
    }

    public void setOptions(List<ConfigOption> options) {
        this.options = options;
    }

    public void listDepartmentOptions() {
        options = getDepartmentOptions(department);
    }

    public void listInstitutionOptions() {
        options = getInstitutionOptions(institution);
    }

    public void listApplicationOptions() {
        options = getApplicationOptions();
    }

    public void listWebUserOptions() {
        options = getWebUserOptions(webUser);
    }

    public boolean isPreventPasswordReuse() {
        return configOptionApplicationController.isPreventPasswordReuse();
    }

    public void setPreventPasswordReuse(boolean value) {
        configOptionApplicationController.setPreventPasswordReuse(value);
    }

    private ConfigOptionFacade getFacade() {
        return optionFacade;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEnumType() {
        return enumType;
    }

    public void setEnumType(String enumType) {
        this.enumType = enumType;
    }

    public String getEnumValue() {
        return enumValue;
    }

    public void setEnumValue(String enumValue) {
        this.enumValue = enumValue;
    }

    public OptionValueType getOptionValueType() {
        return optionValueType;
    }

    public void setOptionValueType(OptionValueType optionValueType) {
        this.optionValueType = optionValueType;
    }

    public List<ConfigOption> getFilteredOptions() {
        return filteredOptions;
    }

    public void setFilteredOptions(List<ConfigOption> filteredOptions) {
        this.filteredOptions = filteredOptions;
    }

    public List<ConfigOption> getSelectedOptions() {
        return selectedOptions;
    }

    public void setSelectedOptions(List<ConfigOption> selectedOptions) {
        this.selectedOptions = selectedOptions;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public List<ConfigOptionDuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public String navigateToDuplicateOptions() {
        detectDuplicateOptions();
        return "/admin/institutions/config_option_duplicates?faces-redirect=true";
    }

    public void detectDuplicateOptions() {
        String jpql = "SELECT o FROM ConfigOption o WHERE o.retired=false ORDER BY o.optionKey, o.scope, o.id";
        List<ConfigOption> all = optionFacade.findByJpql(jpql);
        Map<String, List<ConfigOption>> grouped = all.stream().collect(Collectors.groupingBy(o -> o.getOptionKey() + "|" + o.getScope() + "|"
                + (o.getInstitution() == null ? "null" : o.getInstitution().getId()) + "|"
                + (o.getDepartment() == null ? "null" : o.getDepartment().getId()) + "|"
                + (o.getWebUser() == null ? "null" : o.getWebUser().getId())));
        duplicateGroups = grouped.values().stream()
                .filter(l -> l.size() > 1)
                .map(l -> new ConfigOptionDuplicateGroup(l))
                .collect(Collectors.toList());
    }

    public void retireDuplicateGroup(ConfigOptionDuplicateGroup g) {
        if (g == null || g.getOptions() == null || g.getOptions().size() < 2) {
            return;
        }
        g.getOptions().sort((a, b) -> a.getId().compareTo(b.getId()));
        ConfigOption keep = g.getOptions().get(0);
        for (int i = 1; i < g.getOptions().size(); i++) {
            ConfigOption o = g.getOptions().get(i);
            o.setRetired(true);
            o.setRetiredAt(new Date());
            o.setRetirer(sessionController.getLoggedUser());
            saveOption(o);
        }
        detectDuplicateOptions();
        configOptionApplicationController.loadApplicationOptions();
        JsfUtil.addSuccessMessage("Duplicates retired for " + keep.getOptionKey());
    }

    public static class ConfigOptionDuplicateGroup {

        private List<ConfigOption> options;

        public ConfigOptionDuplicateGroup(List<ConfigOption> options) {
            this.options = options;
        }

        public List<ConfigOption> getOptions() {
            return options;
        }

        public String getOptionKey() {
            return options.get(0).getOptionKey();
        }

        public OptionScope getScope() {
            return options.get(0).getScope();
        }
    }

    @FacesConverter(forClass = ConfigOption.class)
    public static class OptionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            ConfigOptionController controller = (ConfigOptionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "optionController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            return String.valueOf(value);
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof ConfigOption) {
                ConfigOption o = (ConfigOption) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ConfigOption.class.getName());
            }
        }
    }

}
