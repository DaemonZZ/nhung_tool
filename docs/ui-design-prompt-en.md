# UI Design Prompt for the XNT vs Invoice Reconciliation Tool

## Main Prompt

Design a complete UI/UX system for an internal desktop application built with `Kotlin + JavaFX + FXML`. The application is used to automatically reconcile data between:

- the internal `XNT` inventory workbook
- the `Purchase Invoices` workbook
- the `Sales Invoices` workbook

The tool must support these business goals:

1. Compare both `quantity` and `amount` between invoices and XNT.
2. Detect discrepancies in inbound, outbound, and ending inventory.
3. Detect `negative inventory` moments.
4. Warn about `unit of measure mismatches`.
5. Support product-name matching between XNT and invoice data at multiple levels:
   - exact match
   - normalized match
   - AI-suggested match
   - not found in XNT
   - requires manual review
6. Handle items that exist in invoice files but do not exist in XNT.
7. Let users review warnings and perform manual actions before exporting final results.

Design a `proper, fully functional desktop UI` focused on `high-productivity internal workflows` for accounting, tax, and inventory users. Do not make it look like a marketing website or a generic web dashboard. It should feel like a serious data-heavy reconciliation workstation.

## Business Context the UI Must Reflect

The application processes 3 input files:

1. `xnt.xlsx`: the source-of-truth workbook containing the master product list, unit of measure, opening balance, inbound, outbound, and ending balance.
2. the detailed invoice workbook containing 2 sheets:
   - `MuaVao` / Purchase Invoices
   - `BanRa` / Sales Invoices

The application generates 2 output result groups:

1. `Detailed Reconciliation`
   - item-level comparison
   - compares both quantity and amount
   - includes match status and warnings
2. `Negative Inventory Results`
   - shows only actual negative inventory moments
   - includes opening balance, cumulative purchases, cumulative sales, actual negative amount, and same-day sales value

Important business rules:

1. Matching should prioritize `product name`, not depend entirely on product code.
2. The `numeric/model/size/specification` part of a product name is a strong identifier and must not be merged incorrectly.
3. Unit mismatches are `warnings only`, not blocking errors.
4. Products present in invoice files but missing from XNT must still appear in the results.
5. Some cases require manual review before finalizing the output.

## UI Goals

The UI must help users complete this full workflow:

1. Select and load input files.
2. Quickly inspect input data quality.
3. Run the reconciliation process.
4. Review an overview of results.
5. Review warnings and uncertain product mappings.
6. Edit or confirm mapping decisions if needed.
7. Inspect the detailed reconciliation table.
8. Inspect negative inventory results.
9. Filter, search, group, and sort data.
10. Export the final result workbook.
11. Track processing history and logs.

## Required Screen Structure

Design all major screens or panels below:

1. `Main Workspace`
2. `Input File Panel`
3. `Input Data Validation Panel`
4. `Processing Configuration Panel`
5. `Execution Progress Panel`
6. `Results Overview Screen`
7. `Product Mapping Review Screen`
8. `Unit Mismatch Review Screen`
9. `Detailed Reconciliation Screen`
10. `Negative Inventory Screen`
11. `Export Screen`
12. `Run History / Log Screen`
13. `Settings Screen`

## Detailed Requirements by Screen

### 1. Main Workspace

Design a professional desktop workspace optimized for productivity:

- top bar with tool name, active data period, and file status
- left navigation or sidebar for modules
- flexible main content area using tabs or split panes
- bottom status bar showing system state, warning count, progress, and last processing time

Suggested sidebar modules:

- Input
- Data Check
- Run Reconciliation
- Mapping Review
- Warnings
- Detailed Results
- Negative Inventory
- Export
- History
- Settings

### 2. Input File Panel

The UI must allow users to:

- select the `XNT` file
- select the detailed invoice workbook
- automatically detect the `MuaVao` and `BanRa` sheets
- show loaded / not loaded file states
- show metadata such as file name, last modified date, row count, and sheet count
- replace file, clear file, and open source file

Include both a drag-and-drop zone and file picker buttons.

### 3. Input Data Validation Panel

The purpose is to help users quickly validate input quality before running:

- preview the first rows of each sheet
- show valid row count vs invalid row count
- warn about missing required columns
- warn about unexpected sheet format
- warn about blank cells in critical columns
- warn about quantity or amount values that cannot be parsed

Use status badges such as:

- OK
- Warning
- Error

### 4. Processing Configuration Panel

Allow users to configure processing before execution:

- choose the product-name matching mode
- enable or disable AI suggestion
- define the confidence threshold for auto-accepting AI matches
- define the unit mismatch handling rule
- define the default behavior for items not found in XNT
- choose whether to stop for manual review or continue directly

Use a clean structured form with tooltips explaining each option.

### 5. Execution Progress Panel

While processing files, show:

- a clear progress bar
- pipeline stages such as:
  - read files
  - normalize data
  - match products
  - aggregate reconciliation results
  - detect negative inventory
  - generate output workbook
- processed record counts
- short real-time logs
- a cancel or safe stop action if needed

### 6. Results Overview Screen

After processing, provide a clear summary screen showing:

- number of products in XNT
- number of products found in invoices
- number of exact matches
- number of AI-based matches
- number of items requiring review
- number of unit mismatch warnings
- number of items not found in XNT
- number of negative inventory cases
- total inbound discrepancy
- total outbound discrepancy
- total ending balance discrepancy

Design this as summary cards plus a compact summary table plus quick actions.

### 7. Product Mapping Review Screen

This is the most critical screen. Design it for practical high-speed review.

Each row should show:

- product name from invoice data
- matched product name from XNT
- match type: exact / normalized / AI / not found / need review
- confidence score
- highlighted numeric/model/specification parts
- reason for the match or reason for uncertainty
- unit from invoice data
- unit from XNT
- related warnings

Users must be able to:

- confirm a match
- switch to another XNT product
- mark the item as not present in XNT
- ignore a warning
- manually search the XNT catalog
- filter by status
- bulk-approve safe cases

Recommended layout:

- main table in the center
- detail inspector panel on the right
- strong search capability
- filter chips
- bulk action bar

### 8. Unit Mismatch Review Screen

This screen focuses on unit mismatch cases:

- invoice unit
- XNT master unit
- warning type
- risk level
- suggested action

Users should be able to:

- confirm that both units still refer to the same item
- keep the warning but allow export
- send the case to a follow-up list

### 9. Detailed Reconciliation Screen

Design a large, desktop-optimized data table with:

- sticky header
- sortable columns
- resizable columns
- frozen first columns
- multi-condition filters
- quick search
- export current view

The screen must display all key business columns:

- standardized product name
- XNT product name
- invoice product name
- XNT unit
- invoice unit
- match status
- warnings
- opening quantity
- opening amount
- purchase quantity
- purchase amount
- inbound quantity from XNT
- inbound amount from XNT
- purchase vs inbound quantity difference
- purchase vs inbound amount difference
- sales quantity
- sales amount
- outbound quantity from XNT
- outbound amount from XNT
- sales vs outbound quantity difference
- sales vs outbound amount difference
- calculated ending quantity
- calculated ending amount
- XNT ending quantity
- XNT ending amount
- ending quantity difference
- ending amount difference

Use row coloring by severity:

- red for major discrepancies
- yellow for warnings
- neutral or green for good matches

### 10. Negative Inventory Screen

Design a dedicated investigation screen for negative inventory:

- product name
- negative inventory date
- opening balance
- cumulative purchases to date
- cumulative sales to date
- actual negative quantity
- total sales value on that day
- match status
- warnings

Recommended enhancements:

- a mini timeline or small chart for the selected product
- a detail panel explaining why negative inventory occurred
- filters by date, negative magnitude, status, and whether the item exists in XNT

### 11. Export Screen

Allow users to:

- choose the save location
- define the output file name
- export all results or only fully reviewed data
- choose whether to include warning/log sheets
- preview the workbook structure before export

### 12. Run History / Log Screen

This screen should support:

- list of processing runs
- execution time
- user who executed the run
- source files used
- warning counts
- success / failure state
- output file path

Also include:

- detailed logs
- ability to reopen the latest review session if applicable

### 13. Settings Screen

Include system-level settings such as:

- default output folder
- product-name normalization rules
- unit-warning rules
- AI confidence threshold
- small AI model configuration if used
- log retention limit
- optional light/dark theme

## User Experience Requirements

1. Optimize for large desktop screens, around `1366px` and above.
2. Still scale reasonably for laptops.
3. Large data tables are the core UI element and must prioritize readability and fast operation.
4. Warnings and statuses must be immediately visible through color, icon, and badge.
5. Review actions must minimize click count.
6. The workflow should be keyboard-friendly for heavy table usage.
7. Do not use oversized cards or mobile-style spacing.
8. Do not make it visually decorative; prioritize clarity, trust, and enterprise usability.

## Visual Style Requirements

Design direction:

- enterprise desktop
- bright, clear, serious, modern
- strong typographic hierarchy
- restrained use of color
- strong emphasis on processing states and warnings

Avoid:

- landing page style
- flashy gradients
- overly rounded cards
- generic web dashboard aesthetics
- presentation-first layout over operational usability

The UI may take inspiration from:

- data-heavy admin desktop applications
- spreadsheet + inspector panel workflows
- audit / reconciliation workstations

## Technical Constraints for JavaFX / FXML

The design must be realistic for implementation in `JavaFX + FXML`, so:

- prefer layouts that map well to `BorderPane`, `SplitPane`, `VBox`, `HBox`, `TabPane`, and `TableView`
- avoid interactions that rely heavily on web-style animation
- focus on standard desktop components such as:
  - table
  - tree/table
  - form
  - dialog
  - right-side inspector drawer or panel
  - wizard stepper if appropriate

## Expected Output from the Design AI

Return all of the following:

1. Full information architecture for the product.
2. Main user flow from file input to export.
3. Screen list with the purpose of each screen.
4. Detailed wireframe description for each screen.
5. Specific desktop layout recommendations.
6. Component inventory.
7. Color rules for statuses:
   - success
   - warning
   - error
   - review needed
   - AI suggested
8. Recommendations for displaying large tabular datasets.
9. Recommended user actions for the review screens.
10. A design direction detailed enough for developers to rebuild in JavaFX/FXML.

## Preferred Response Structure for the Design AI

If the AI responds in descriptive form, organize it in this order:

1. Design concept
2. Information architecture
3. Primary user flow
4. Screen-by-screen breakdown
5. Key components
6. Interaction rules
7. Visual system
8. Notes for JavaFX/FXML implementation

If the AI responds with mockups or wireframes, prioritize:

- clear layout
- large data tables
- a right-side review panel
- a summary header
- explicit status badges, filters, and action controls

## Additional Requirement

The UI must clearly communicate that this is a serious workflow tool for `validation`, `reconciliation`, `review`, and `report export`, not just a simple file-upload screen.

The design must make the three work layers obvious:

1. `Load and validate data`
2. `Reconcile and review`
3. `Inspect results and export reports`

Prioritize a continuous workflow with minimal context switching, minimal unnecessary popups, and strong support for doing most work directly inside the main workspace.

## Short Prompt

Design a complete UI/UX system for an internal desktop application built with Kotlin + JavaFX + FXML that reconciles data between an XNT inventory workbook, purchase invoice data, and sales invoice data. The application must support file import, input validation preview, reconciliation pipeline execution, product-name review across exact/normalized/AI match states, unit mismatch warnings, handling items that exist in invoices but not in XNT, detailed result tables comparing both quantity and amount, negative inventory analysis by date, filter/search/sort/bulk actions/export/log/history/settings. The style must be enterprise desktop, data-heavy, optimized for accounting/tax/inventory users, and realistic to implement in JavaFX/FXML. Do not make it look like a generic web dashboard or landing page.

