---
name: Precision Enterprise Workstation
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#45464d'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#76777d'
  outline-variant: '#c6c6cd'
  surface-tint: '#565e74'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#131b2e'
  on-primary-container: '#7c839b'
  inverse-primary: '#bec6e0'
  secondary: '#515f74'
  on-secondary: '#ffffff'
  secondary-container: '#d5e3fd'
  on-secondary-container: '#57657b'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#271901'
  on-tertiary-container: '#98805d'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#d5e3fd'
  secondary-fixed-dim: '#b9c7e0'
  on-secondary-fixed: '#0d1c2f'
  on-secondary-fixed-variant: '#3a485c'
  tertiary-fixed: '#fcdeb5'
  tertiary-fixed-dim: '#dec29a'
  on-tertiary-fixed: '#271901'
  on-tertiary-fixed-variant: '#574425'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  tabular-nums:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  table-row-height: 28px
  header-height: 40px
  pane-gutter: 1px
  container-padding: 12px
  component-gap: 8px
---

## Brand & Style
This design system is engineered for high-stakes enterprise environments where data density and cognitive clarity are paramount. The brand personality is authoritative, systematic, and utilitarian, evoking the feel of a high-performance workstation rather than a consumer application. 

The aesthetic follows a **Corporate / Modern** approach with a lean toward **Structural Minimalism**. It prioritizes information architecture over decorative flourishes. Visual weight is used strategically to guide the eye through complex datasets, using a "form follows function" philosophy tailored for JavaFX-based desktop applications. The goal is to reduce "UI noise," allowing the user’s data to remain the primary focus.

## Colors
The palette is built on a foundation of professional slates and grays to ensure long-term visual comfort during extended work sessions. 

- **Primary & Secondary:** Deep slates (#0f172a, #334155) provide a strong anchoring effect for navigation and headers.
- **Surface Palette:** A range of ultra-light grays (#f8fafc to #f1f5f9) differentiates between various functional zones like sidebars, inspectors, and main content stages.
- **Functional Accents:** Status colors are high-chroma to ensure they remain legible even at small sizes (e.g., 8px indicators). 
- **AI Integration:** Features driven by machine learning are designated with a Soft Purple (#8b5cf6) to distinguish automated suggestions from human-verified data.

## Typography
Typography is optimized for a high-density "Inter" font implementation. The system relies on a strict hierarchy to manage complex information without increasing font size unnecessarily.

- **Data Tables:** Use `body-sm` or `tabular-nums`. Tabular numbers are mandatory for all financial or metric columns to ensure vertical alignment.
- **Labels:** Use `label-caps` for section headers within `Inspector` panels or `SplitPane` titles to provide clear categorization without occupying vertical space.
- **Hierarchy:** Weight (Semi-bold vs. Regular) is the primary tool for differentiation rather than scale.

## Layout & Spacing
The layout utilizes a **Fixed/Structured Grid** specifically optimized for FXML `BorderPane` and `SplitPane` architectures.

- **Layout Model:** 
    - **Top:** Global Menu and Navigation (`BorderPane.top`).
    - **Left:** Navigation Tree or Project Explorer.
    - **Center:** Multi-tabbed `TableView` or `Dashboard`.
    - **Right:** Contextual `Inspector` panel for property editing.
- **Rhythm:** A 4px baseline grid governs all spacing. In high-density mode, row heights in `TableView` are constrained to 28px to maximize the number of visible records.
- **Splitters:** `SplitPane` dividers should be minimal (1px to 2px) with a subtle hover state to indicate resizability.

## Elevation & Depth
Depth is conveyed through **Tonal Layering** and **Low-Contrast Outlines** rather than shadows. This minimizes visual clutter in complex layouts.

- **Backgrounds:** The furthest back layer (the Window background) uses a slightly darker gray. Content areas (Cards, Tables) use a pure white (#FFFFFF) or light slate surface.
- **Borders:** All UI components are contained by 1px borders (#e2e8f0). Active or focused elements use a 1px primary-colored border.
- **Stacked Depth:** Modals or pop-overs use a very subtle, tight shadow (0px 2px 4px rgba(0,0,0,0.1)) to provide just enough separation from the data grid beneath.

## Shapes
The shape language is rigid and professional. 
- **Corner Radius:** A universal 4px (`rounded-sm`) is applied to buttons, input fields, and containers. 
- **Tabs:** `TabPane` headers use 0px bottom corners to maintain a seamless connection with the content area.
- **Badges:** Small status badges use a slightly higher radius (6px) or are kept square to maintain the workstation aesthetic.

## Components
Consistent implementation of these components ensures a predictable user experience:

- **TableView:** Must support frozen columns (left-aligned) for ID/Name fields. Column headers are flat, using a `neutral-light` background and `label-caps` typography. Row selection uses a light blue tint (#f1f5f9) with a 2px primary-color left border.
- **Buttons:** Compact (24px - 30px height). Primary buttons use a solid slate background. Secondary buttons use a "Ghost" style with a 1px border.
- **Input Fields:** Flat styling with a 1px border. Focus state is indicated by a primary-color border and a 1px inner glow.
- **Status Badges:** Compact pills with low-opacity backgrounds (10-15%) and high-contrast text using the functional status colors defined in the Color section.
- **Inspector Panels:** Vertical stacks of "Label: Input" pairs. Use `body-sm` for labels to allow for long property names.
- **Tabs:** Square, high-contrast tabs with a primary-colored top-border indicator for the active state.
- **TreeViews:** Minimalist icons (12px) with horizontal dotted lines for hierarchy visualization (standard workstation pattern).