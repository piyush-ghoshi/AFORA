---
name: Academic Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf6'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dde9ff'
  surface-container-highest: '#d3e3ff'
  on-surface: '#0b1c30'
  on-surface-variant: '#464553'
  inverse-surface: '#213146'
  inverse-on-surface: '#ebf1ff'
  outline: '#777584'
  outline-variant: '#c8c4d5'
  surface-tint: '#544fc0'
  primary: '#1f108e'
  on-primary: '#ffffff'
  primary-container: '#3730a3'
  on-primary-container: '#a9a7ff'
  inverse-primary: '#c3c0ff'
  secondary: '#5d5c76'
  on-secondary: '#ffffff'
  secondary-container: '#e2dfff'
  on-secondary-container: '#63627d'
  tertiary: '#003512'
  on-tertiary: '#ffffff'
  tertiary-container: '#004e1e'
  on-tertiary-container: '#48c768'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e2dfff'
  primary-fixed-dim: '#c3c0ff'
  on-primary-fixed: '#0f0069'
  on-primary-fixed-variant: '#3b35a7'
  secondary-fixed: '#e2dfff'
  secondary-fixed-dim: '#c6c3e2'
  on-secondary-fixed: '#1a1930'
  on-secondary-fixed-variant: '#45445e'
  tertiary-fixed: '#7ffc97'
  tertiary-fixed-dim: '#62df7d'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005320'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e3ff'
typography:
  display-lg:
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
    letterSpacing: -0.01em
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
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: -0.01em
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 0.75rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system delivers an institutional-grade, data-dense utility interface tailored for higher education attendance tracking, lecture verification, and student administration. The design language marries the high-efficiency craft of modern software engineering tools (e.g., Linear) with the grounded authority of university registrar standards. 

The emotional signature is quiet confidence, institutional trust, and analytical clarity. Rather than decorative visual effects, it relies on strict typographic rhythm, generous negative space balanced against dense table structures, crisp 1-pixel borders, and immediate semantic signaling. Structural clarity replaces visual fluff to reduce cognitive fatigue for professors, registrars, and students operating on mobile devices in fast-paced classroom environments.

## Colors

The palette establishes an austere, administrative foundation punctuated by high-contrast functional color codings. 

### Canvas & Structural Neutrals
- **Background Canvas**: `#F8F9FF` (a cool, crisp institutional off-white that minimizes glare under classroom fluorescent lighting).
- **Surface / Card Background**: `#FFFFFF`.
- **Structural Border**: `#E2E8F0` (used for 1px perimeter outlines on cards, headers, row dividers, and input strokes).
- **Subtle Fill / Row Hover**: `#F1F5F9`.

### Typography Neutrals
- **Text Primary**: `#0B1C30` (deep ink-black; ensures maximum contrast and passes WCAG AAA on white surfaces).
- **Text Secondary / Muted**: `#464553` (used for metadata, table headers, and supporting captions).
- **Outline / De-emphasized Text**: `#777584` (disabled states, inactive placeholders, and structural dividers).

### Brand Identity
- **Primary Indigo**: `#3730A3` (used for primary actions, selected navigation items, and active states).
- **On-Primary**: `#FFFFFF`.
- **Primary Container**: `#E2DFFF` (used for active pill highlights and subtle selected background states).
- **On-Primary Container**: `#1E1B4B`.

### Semantic Status Colors
Status indicators are used selectively for small status badges, circular pips, and vertical 4px card anchor borders:
- **Present**: `#16A34A` (Container: `#DCFCE7`, On-Container: `#14532D`)
- **Late**: `#EAB308` (Container: `#FEF9C3`, On-Container: `#713F12`)
- **Absent**: `#DC2626` (Container: `#FEE2E2`, On-Container: `#7F1D1D`)
- **On Leave**: `#3B82F6` (Container: `#DBEAFE`, On-Container: `#1E3A8A`)
- **Pending / In Review**: `#D97706` (Container: `#FEF3C7`, On-Container: `#78350F`)
- **Cancelled / Excused**: `#64748B` (Container: `#F1F5F9`, On-Container: `#334155`)

### Error & Destructive
- **Error**: `#BA1A1A`
- **On-Error**: `#FFFFFF`
- **Error Container**: `#FFDAD6`
- **On-Error Container**: `#410002`

## Typography

Typography prioritizes tabular legibility and strict visual hierarchy.

- **Primary Typeface**: `Inter` handles all structural chrome, body text, headlines, and general labels. Headings utilize tight letter-spacing to emphasize architectural solidity.
- **Monospace Engine**: `JetBrains Mono` is enforced for academic identifiers, student roll numbers, section codes, time codes (e.g., `09:00:00`), OTPs, and attendance verification PINs. This guarantees character alignment in vertically stacked lists.
- **Micro-Copy Rules**: All status badges, metadata labels, and column headers use `label-sm` or `label-md` with slight positive tracking (`+0.02em` to `+0.04em`) to maintain legibility at 11–12px on compact mobile displays.

## Layout & Spacing

The target platform is Android portrait at an initial reference viewport of 360x800dp.

### Layout Rhythm
- **Canvas Margins**: Fixed at `16px` (`1rem`) on left and right edges, preserving maximum usable surface area for compact mobile data tables while ensuring clean separation from system bezels.
- **Column Structure**: Built on a 4-column fluid layout for mobile phone screens with `12px` (`0.75rem`) gutters.
- **Vertical Rhythm**: Strictly multiples of `4px` and `8px`. Dense data rows occupy a vertical height of `48px` to `56px`, honoring tactile accessibility while avoiding excessive scroll length.
- **Safe Area Insets**: Top app bars reserve `48dp` for system status bars and `56dp` for application navigation. Bottom navigation bars occupy `64dp` plus the Android system navigation bar inset.

## Elevation & Depth

This design system avoids diffused ambient shadows, skeletal blur radii, and skeuomorphic drop-shadows. Visual depth is established through a strict flat architecture:

1. **Surface Separation via Crisp Outlines**: Layers are differentiated through `1px solid #E2E8F0` outlines rather than shadows.
2. **Tonal Planar Stacking**:
   - Level 0 (Base Canvas): `#F8F9FF`
   - Level 1 (Card / Content Surface): `#FFFFFF` enclosed in `#E2E8F0` borders
   - Level 2 (Sticky Headers & Toolbars): `#FFFFFF` with a bottom border of `1px solid #E2E8F0`
   - Level 3 (Modals / Bottom Sheets): `#FFFFFF` with a top perimeter border of `1px solid #CBD5E1` and an overlay backdrop of `#0B1C30` at 32% opacity.
3. **Zero Shadow Policy**: Elevation shadows are set to `none` (`box-shadow: 0 0 0 0 transparent`). Component hierarchy relies on surface contrast and structural borders.

## Shapes

The geometric framework balances technical rigor with modern ergonomics:

- **Buttons & Form Inputs**: Set strictly to `4px` border radius (`rounded-sm`). This provides an intentional, instrument-like feel.
- **Cards, Modals & Surfaces**: Set to `8px` border radius (`rounded-md`).
- **Semantic Pips, Status Dots & User Avatars**: Set to full circles (`9999px` / `rounded-full`).
- **Status Edge Indicators**: Embedded flush within card left edges as a vertical `4px` colored rectangle, masking precisely to the parent card’s `8px` top-left and bottom-left bounds.

## Components

### Buttons
- **Primary Button**: Solid fill `#3730A3`, text `#FFFFFF`, border-radius `4px`, height `48px`, font `Inter` 14px Weight 600. No shadow. Active state shifts background to `#312E81`.
- **Secondary / Outlined Button**: Background `#FFFFFF`, border `1px solid #E2E8F0`, text `#0B1C30`, border-radius `4px`, height `48px`. Active state applies background `#F8F9FF`.
- **Destructive Button**: Background `#BA1A1A`, text `#FFFFFF`, border-radius `4px`, height `48px`.

### Attendance Cards & List Items
- **Structure**: White `#FFFFFF` background, `1px solid #E2E8F0` perimeter border, `8px` border-radius.
- **Semantic Accent**: A `4px` solid border strip on the extreme left edge matching the student or session state (e.g., `#16A34A` for Present, `#DC2626` for Absent).
- **Layout**: Student name rendered in `body-md` (Inter 14/600), student roll/matriculation number rendered in `code-md` (JetBrains Mono 13/500).
- **Touch Targets**: Row height minimum `56px` to ensure effortless one-handed roll-call toggling.

### Status Chips & Badges
- **Dimensions**: Height `22px`, internal padding `2px 8px`, border-radius `4px`.
- **Typography**: `label-sm` (Inter 11px Weight 600, uppercase).
- **Coloring**: High-contrast pairing using semantic containers and dark text (e.g., Present badge uses background `#DCFCE7`, text `#14532D`, border `1px solid #BBF7D0`). Every status badge must include both a `6px` circular pip and an unambiguous text label.

### Input Fields & Search Bars
- **Surface**: Background `#FFFFFF`, border `1px solid #777584`, border-radius `4px`, height `48px`.
- **Focus State**: Border shifts to `2px solid #3730A3` with zero drop shadow.
- **Typography**: Value text in `Inter` 14px `#0B1C30`. Numeric or ID fields invoke `JetBrains Mono`. Floating label rendered in `label-sm` `#464553`.

### Selection Controls (Checkboxes & Radios)
- **Geometry**: Checkbox has `4px` border radius; radio is fully circular.
- **Dimensions**: Visual mark `20x20px`, seated inside an invisible `48x48px` touch target.
- **Unchecked**: Border `1.5px solid #777584`, background `#FFFFFF`.
- **Checked**: Border and background `#3730A3`, icon glyph in `#FFFFFF`.

### Metric & Attendance Stat Banners
- **Grid Tiles**: 2-column card layouts with `#FFFFFF` background, `1px solid #E2E8F0` border, `8px` radius, padding `12px`.
- **Content**: Stat value rendered in `Display` or `Headline-lg` (Inter 24px/600 or JetBrains Mono 24px/600 for percentages), paired with a `label-md` uppercase category descriptor in `#464553`.