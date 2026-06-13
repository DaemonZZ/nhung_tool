---
name: Precision Data System
colors:
  surface: '#f8fafb'
  surface-dim: '#d8dadb'
  surface-bright: '#f8fafb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f5'
  surface-container: '#eceeef'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#454652'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#eff1f2'
  outline: '#757684'
  outline-variant: '#c5c5d4'
  surface-tint: '#4355b9'
  primary: '#24389c'
  on-primary: '#ffffff'
  primary-container: '#3f51b5'
  on-primary-container: '#cacfff'
  inverse-primary: '#bac3ff'
  secondary: '#006a6a'
  on-secondary: '#ffffff'
  secondary-container: '#90efef'
  on-secondary-container: '#006e6e'
  tertiary: '#6c3400'
  on-tertiary: '#ffffff'
  tertiary-container: '#8f4700'
  on-tertiary-container: '#ffc7a2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dee0ff'
  primary-fixed-dim: '#bac3ff'
  on-primary-fixed: '#00105c'
  on-primary-fixed-variant: '#293ca0'
  secondary-fixed: '#93f2f2'
  secondary-fixed-dim: '#76d6d5'
  on-secondary-fixed: '#002020'
  on-secondary-fixed-variant: '#004f4f'
  tertiary-fixed: '#ffdcc6'
  tertiary-fixed-dim: '#ffb784'
  on-tertiary-fixed: '#301400'
  on-tertiary-fixed-variant: '#713700'
  background: '#f8fafb'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: -0.01em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  label-md:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.02em
  mono-data:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: -0.01em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  container-padding: 8px
  gutter: 4px
  component-gap: 4px
  density-multiplier: '0.5'
---

## Brand & Style
This design system is engineered for high-performance internal environments where data throughput is the primary metric of success. The brand personality is **utilitarian, precise, and authoritative**, removing all non-functional decorative elements to reduce cognitive load during long-duration work sessions.

The aesthetic follows a **Modern Corporate** approach with a focus on structural clarity. It draws inspiration from technical IDEs and financial terminals, prioritizing a "single-pane-of-glass" philosophy. The emotional response should be one of control and reliability, ensuring users feel equipped to handle complex datasets without visual distraction.

## Colors
The palette is strictly functional, mapping colors to system states rather than brand expression. 

- **Teal (#008080):** Reserved exclusively for AI-generated matches, automated suggestions, and machine-learning confidence indicators.
- **Indigo (#3F51B5):** Indicates elements requiring manual review, human intervention, or user-owned actions.
- **Crimson (#DC143C):** System errors, critical data validation failures, and destructive actions.
- **Amber (#FFBF00):** Warnings, non-blocking validation issues, and pending states.

The background uses a tiered grayscale to define hierarchy without relying on shadows, ensuring the UI remains crisp on high-resolution displays.

## Typography
The system utilizes **Inter** with a condensed approach to spacing. To maximize data density, we utilize a 13px base for standard body text and 12px for data tables. 

Key typographic principles:
- **Tight Leading:** Line heights are kept to a minimum (1.3x - 1.4x) to allow more rows of data to be visible above the fold.
- **Negative Tracking:** Headlines and medium-sized labels use slight negative letter spacing to improve word-shape recognition at small sizes.
- **State-Heavy Labels:** Micro-labels (11px) are used for metadata and status indicators to keep the primary visual path clear.

## Layout & Spacing
The layout system is modeled after JavaFX structures to facilitate complex, multi-panel application states.

- **BorderPane:** The root structure. The `Top` region is reserved for global toolbars, `Left` for navigation trees, `Center` for the primary data workspace, and `Right` for contextual inspectors.
- **SplitPane:** All major view boundaries must use 2px draggable dividers, allowing users to customize their workspace according to their specific monitor configuration.
- **TabPane:** Used within the `Center` region to manage multiple active work items. Tabs are square and docked to the top of the content area.

We utilize a **4px base grid**. Horizontal and vertical padding within data cells is strictly limited to 4px or 8px to ensure maximum information density.

## Elevation & Depth
This design system avoids ambient shadows in favor of **Tonal Layering and Stroke-based boundaries**. This ensures that even at high zoom levels or on lower-quality panels, the interface remains sharp.

- **Surface Levels:** The base application background is the darkest neutral. Each nested container (SplitPane partitions) uses a slightly lighter gray to indicate foreground proximity.
- **Borders:** 1px solid borders (Neutral-300) are used to define every functional area. 
- **Active State:** Focus and active selections are indicated by a 2px interior stroke in the Primary (Indigo) or Secondary (Teal) color, rather than an outer glow.

## Shapes
Shapes are intentionally geometric and "Soft-Sharp." 

A global radius of **4px (0.25rem)** is applied to buttons, input fields, and container corners. This small radius provides just enough modern refinement to prevent the UI from feeling dated, without wasting the pixel real estate required by larger, rounded "pill" shapes. Interactive regions should never use a radius larger than 4px.

## Components
- **Data Grids:** The core component. Rows must be 28px in height. Alternating row stripes are required for readability. Cell editing is triggered by a single click to minimize friction.
- **Action Buttons:** Small (24px height) with icon-only or short text labels. Use a subtle gradient to indicate "pressable" depth without breaking the flat aesthetic.
- **Status Chips:** Small, rectangular badges with 2px rounded corners. Use the functional color palette (Crimson, Amber, Indigo, Teal) for the background with high-contrast white text.
- **Input Fields:** Inset styling with a 1px border. Labels should be positioned above the field or as a placeholder to save horizontal space.
- **Splitters:** 2px wide, containing a centered "handle" of 3 dots. Hovering over a splitter changes the cursor to the appropriate resize icon immediately.
- **Tree Views:** Used in the `Left` BorderPane region for hierarchical navigation. Use 16px indentations per level.