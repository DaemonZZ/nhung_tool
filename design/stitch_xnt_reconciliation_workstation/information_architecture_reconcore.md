# ReconCore: Enterprise Reconciliation Workstation
## Information Architecture & User Flow

### 1. Load & Validate (Data Ingestion)
- **Input File Panel:** Drag-and-drop for XNT workbook and invoice files. Metadata detection.
- **Validation Panel:** Real-time parsing of column headers, data types, and missing critical values.

### 2. Reconcile & Review (Decision Layer)
- **Configuration:** Set AI confidence and unit matching rules.
- **Product Mapping Review:** The core workspace. Side-by-side comparison of Invoice vs XNT names with AI confidence scores.
- **Unit Mismatch Review:** Isolated view for unit-of-measure discrepancies.

### 3. Results & Export (Output Layer)
- **Detailed Reconciliation:** The master "Mega Table" with 20+ columns for audit-ready comparison.
- **Negative Inventory:** Chronological view of balance dips.
- **Export:** Final workbook generation.

### Key UX Patterns
- **Inspector Panel:** Right-aligned sidebar that updates based on table row selection.
- **Status Badging:** Universal color system for discrepancy levels.
- **Bulk Actions:** Sticky footer/header for rapid confirmation.
- **Data Density:** 12pt type, tight cell padding, optimized for 1440p+ screens.