---
name: Academic Precision
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
  on-surface-variant: '#464553'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#777584'
  outline-variant: '#c8c4d5'
  surface-tint: '#544fc0'
  primary: '#1f108e'
  on-primary: '#ffffff'
  primary-container: '#3730a3'
  on-primary-container: '#a9a7ff'
  inverse-primary: '#c3c0ff'
  secondary: '#4648d4'
  on-secondary: '#ffffff'
  secondary-container: '#6063ee'
  on-secondary-container: '#fffbff'
  tertiary: '#511c00'
  on-tertiary: '#ffffff'
  tertiary-container: '#752c00'
  on-tertiary-container: '#fe9562'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3b35a7'
  secondary-fixed: '#e1e0ff'
  secondary-fixed-dim: '#c0c1ff'
  on-secondary-fixed: '#07006c'
  on-secondary-fixed-variant: '#2f2ebe'
  tertiary-fixed: '#ffdbcc'
  tertiary-fixed-dim: '#ffb694'
  on-tertiary-fixed: '#351000'
  on-tertiary-fixed-variant: '#7a3003'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 32px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style
The design system is built for a Smart Classroom Attendance Management System, prioritizing extreme clarity, high-speed interaction, and academic authority. The visual direction merges the utility-first approach of **Minimalism** with the refined depth of **Corporate Modern** design.

The personality is professional, data-driven, and unobtrusive. It aims to evoke a sense of reliability and institutional trust, ensuring that educators can manage complex attendance data without cognitive overload. The aesthetic utilizes generous whitespace, crisp borders, and a monochromatic foundation to allow primary and semantic colors to carry the functional weight of the UI.

## Colors
This design system employs a "Focused Palette" strategy. The primary **Indigo (#3730A3)** is reserved for high-level branding and primary action states. The **Slate** grayscale handles 90% of the interface, creating a calm, non-distracting environment.

Semantic colors are crucial for this application. Use **Success (Green)** for "Present," **Warning (Amber)** for "Late," and **Critical (Red)** for "Absent." These colors must be used sparingly as indicators (badges or pips) to maintain a professional atmosphere and avoid a "cluttered" visual heat map.

## Typography
The typography is centered on **Inter**, a typeface designed for screen legibility and UI density. For technical identifiers (Student IDs, Serial Numbers), use **JetBrains Mono** to distinguish data from prose.

- **Headlines:** Use tight letter spacing for a modern, "Linear-like" feel.
- **Body:** Standard body text uses `body-md` (14px) to maximize information density in tables and lists.
- **Labels:** Uppercase or medium-weight labels should be used for metadata and table headers to provide a clear structural hierarchy.

## Layout & Spacing
The layout follows a **Fixed-Fluid Hybrid** model. Dashboards utilize a 12-column grid with a fixed maximum width of 1280px to ensure readability on wide monitors.

- **Density:** High density is preferred. Use an 8px base grid for component alignment, but allow 4px increments for fine-tuning data tables.
- **Mobile:** Transition to a single-column stack with 16px horizontal margins. Cards should lose their shadows and use simple 1px borders on mobile to maximize screen real estate.
- **Navigation:** A persistent left-hand sidebar (240px) is standard for desktop, collapsing into a bottom bar or "hamburger" menu on mobile.

## Elevation & Depth
The design system avoids heavy shadows, opting for **Tonal Layers** and **Low-contrast outlines**.

- **Level 0 (Canvas):** Background color `#F8FAFC`.
- **Level 1 (Cards/Surface):** White background with a 1px border (`#E2E8F0`). No shadow for static elements.
- **Level 2 (Interactive):** Elements that are clickable or require focus use a very soft, diffused shadow: `0px 1px 3px rgba(0, 0, 0, 0.1)`.
- **Level 3 (Modals/Popovers):** Use a crisp 1px border with a larger, neutral shadow to separate the element from the page background.

Depth is primarily communicated through color layering (e.g., a darker slate header over a lighter slate sidebar) rather than physical extrusion.

## Shapes
Following the professional aesthetic, shapes are **Soft** but not overly rounded. 

- **Components:** Standard buttons and input fields use `rounded` (0.25rem).
- **Containers:** Large cards and modals use `rounded-lg` (0.5rem) to provide a gentle containerized feel.
- **Indicators:** Status pips and user avatars should remain perfectly circular (pill-shaped) to distinguish them from structural UI elements.

## Components
### Buttons
- **Primary:** Solid `#3730A3` with white text. High contrast, 1px inset border for a tactile "pressed" feel.
- **Secondary:** White background with `#E2E8F0` border and `#334155` text.
- **Ghost:** No border or background, only text; turns to a subtle gray background on hover.

### Data Tables
Tables are the core of the attendance system. 
- **Rows:** 48px height, 1px bottom border only. 
- **Hover state:** Row background changes to `#F1F5F9`.
- **Status Badges:** Small, subtle background tint with high-contrast text (e.g., Success: Light green background, dark green text).

### Form Fields
- **Inputs:** 1px border (`#CBD5E1`). On focus, the border changes to the primary indigo with a 2px outer "glow" (soft shadow) of the same color at 20% opacity.

### Attendance Cards
For mobile-friendly views, use cards with a clear "Status Bar" on the left edge. The color of the bar (Green/Amber/Red) should immediately communicate the attendance state without needing to read the text.

### Visualizations
Charts (Attendance trends) should use the Primary Indigo for the main data line and light Slate for grid lines. Avoid multi-color charts unless representing distinct semantic categories.