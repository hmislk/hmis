import pandas as pd
import requests
import json
import time
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8080/sl"
API_KEY = "bdc3775d-f07a-4c15-855d-9e201fa4af84"
DEPARTMENT_ID = 1048776
DEPARTMENT_NAME = "ETU - Diagnostic Centre"

HEADERS = {
    "Finance": API_KEY,
    "Content-Type": "application/json"
}

def search_stocks(query, include_zero=True, batch_no=None):
    """Search for stocks by item name/code"""
    url = f"{BASE_URL}/api/pharmacy_adjustments/search/stocks"
    params = {
        "query": query,
        "department": DEPARTMENT_NAME,
        "includeZeroStock": str(include_zero).lower(),
        "limit": 100
    }
    if batch_no:
        params["batchNo"] = batch_no

    try:
        response = requests.get(url, headers=HEADERS, params=params)
        data = response.json()
        if data.get("status") == "success":
            return data.get("data", [])
        else:
            print(f"  Error searching: {data.get('message')}")
            return []
    except Exception as e:
        print(f"  Exception: {e}")
        return []

def adjust_stock_quantity(stock_id, new_quantity, comment):
    """Adjust stock quantity to a new value"""
    url = f"{BASE_URL}/api/pharmacy_adjustments/stock_quantity"
    payload = {
        "stockId": stock_id,
        "newQuantity": new_quantity,
        "comment": comment,
        "departmentId": DEPARTMENT_ID
    }

    try:
        response = requests.post(url, headers=HEADERS, json=payload)
        data = response.json()
        if data.get("status") == "success":
            return True, data.get("data")
        else:
            return False, data.get("message")
    except Exception as e:
        return False, str(e)

def phase1_zero_all_stocks():
    """Phase 1: Set all current stocks to zero"""
    print("\n" + "="*80)
    print("PHASE 1: Setting all current stocks to ZERO")
    print("="*80)

    # Read current stock file
    df = pd.read_excel(r'D:\OneDrive - University of Colombo\Desktop\tmp\current_stock_by_batch.xlsx', header=1)

    # Get items with stock > 0
    df['Stock_num'] = pd.to_numeric(df['Stock'], errors='coerce')
    df_with_stock = df[df['Stock_num'] > 0].copy()

    print(f"Total items to process: {len(df_with_stock)}")

    success_count = 0
    fail_count = 0
    already_zero = 0
    processed_stock_ids = set()  # Track processed stocks to avoid duplicates

    for idx, row in df_with_stock.iterrows():
        item_name = str(row['Item'])
        item_code = str(row['Code']) if pd.notna(row['Code']) else None
        batch_no = str(row['Batch No']) if pd.notna(row['Batch No']) else None
        current_stock = row['Stock_num']

        print(f"\n[{idx+1}] Processing: {item_name}")
        print(f"    Code: {item_code}, Batch: {batch_no}, Current Stock: {current_stock}")

        # Search by code first (more specific), then by name
        search_term = item_code if item_code and item_code != 'nan' else item_name

        stocks = search_stocks(search_term, include_zero=False, batch_no=batch_no)

        if not stocks:
            # Try searching by name if code didn't work
            stocks = search_stocks(item_name, include_zero=False)

        if not stocks:
            print(f"    WARNING: No stocks found for this item")
            fail_count += 1
            continue

        # Process each matching stock
        for stock in stocks:
            stock_id = stock['stockId']

            # Skip if already processed
            if stock_id in processed_stock_ids:
                continue

            processed_stock_ids.add(stock_id)
            current_qty = stock['stockQty']

            if current_qty == 0:
                print(f"    Stock ID {stock_id} already at zero, skipping")
                already_zero += 1
                continue

            # Set to zero
            success, result = adjust_stock_quantity(
                stock_id,
                0.0,
                f"Stock reset - Phase 1: Zeroing all stocks for inventory reset"
            )

            if success:
                print(f"    SUCCESS: Stock ID {stock_id} set to 0 (was {current_qty})")
                success_count += 1
            else:
                print(f"    FAILED: Stock ID {stock_id} - {result}")
                fail_count += 1

            time.sleep(0.1)  # Small delay to avoid overwhelming the server

    print("\n" + "-"*80)
    print(f"PHASE 1 COMPLETE:")
    print(f"  Successful adjustments: {success_count}")
    print(f"  Already at zero: {already_zero}")
    print(f"  Failed: {fail_count}")
    print("-"*80)

    return processed_stock_ids

def phase2_set_correct_stocks():
    """Phase 2: Set stocks to correct values"""
    print("\n" + "="*80)
    print("PHASE 2: Setting correct stock values")
    print("="*80)

    # Read correct stock file
    df = pd.read_excel(r'D:\OneDrive - University of Colombo\Desktop\tmp\correct_stock.xlsx', header=1)

    # Get items with non-NaN stock values > 0
    df['Stock_num'] = pd.to_numeric(df['Stock'], errors='coerce')
    df_with_stock = df[pd.notna(df['Stock_num']) & (df['Stock_num'] > 0)].copy()

    print(f"Total items to set correct stock: {len(df_with_stock)}")

    success_count = 0
    fail_count = 0
    not_found = 0

    for idx, row in df_with_stock.iterrows():
        item_name = str(row['Item'])
        item_code = str(row['Code']) if pd.notna(row['Code']) else None
        batch_no = str(row['Batch No']) if pd.notna(row['Batch No']) else None
        target_stock = row['Stock_num']

        print(f"\n[{idx+1}] Setting: {item_name}")
        print(f"    Code: {item_code}, Batch: {batch_no}, Target Stock: {target_stock}")

        # Search by code first
        search_term = item_code if item_code and item_code != 'nan' else item_name

        stocks = search_stocks(search_term, include_zero=True, batch_no=batch_no)

        if not stocks:
            stocks = search_stocks(item_name, include_zero=True)

        if not stocks:
            print(f"    WARNING: No stocks found for this item")
            not_found += 1
            continue

        # Find the best matching stock (by batch number if specified)
        selected_stock = None

        if batch_no and batch_no != 'nan':
            # Try to match by batch number
            for stock in stocks:
                stock_batch = stock.get('batchNo', '')
                if batch_no in str(stock_batch) or str(stock_batch) in batch_no:
                    selected_stock = stock
                    break

        if not selected_stock:
            # Just take the first one
            selected_stock = stocks[0]

        stock_id = selected_stock['stockId']
        current_qty = selected_stock['stockQty']

        # Set to correct value
        success, result = adjust_stock_quantity(
            stock_id,
            target_stock,
            f"Stock reset - Phase 2: Setting correct stock value"
        )

        if success:
            print(f"    SUCCESS: Stock ID {stock_id} set to {target_stock} (was {current_qty})")
            success_count += 1
        else:
            print(f"    FAILED: Stock ID {stock_id} - {result}")
            fail_count += 1

        time.sleep(0.1)

    print("\n" + "-"*80)
    print(f"PHASE 2 COMPLETE:")
    print(f"  Successful adjustments: {success_count}")
    print(f"  Not found: {not_found}")
    print(f"  Failed: {fail_count}")
    print("-"*80)

def main():
    print("="*80)
    print("STOCK RESET SCRIPT FOR ETU - Diagnostic Centre")
    print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*80)

    # Test API connectivity
    print("\nTesting API connectivity...")
    test_stocks = search_stocks("Aciloc", include_zero=True)
    if test_stocks:
        print(f"API OK - Found {len(test_stocks)} test stocks")
    else:
        print("WARNING: API test returned no results")

    # Phase 1: Zero all stocks
    phase1_zero_all_stocks()

    # Phase 2: Set correct values
    phase2_set_correct_stocks()

    print("\n" + "="*80)
    print(f"SCRIPT COMPLETED at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*80)

if __name__ == "__main__":
    main()
