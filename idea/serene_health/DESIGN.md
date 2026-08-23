---
name: Serene Health
colors:
  surface: '#faf9f6'
  surface-dim: '#dbdad7'
  surface-bright: '#faf9f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f1'
  surface-container: '#efeeeb'
  surface-container-high: '#e9e8e5'
  surface-container-highest: '#e3e2e0'
  on-surface: '#1a1c1a'
  on-surface-variant: '#3e494a'
  inverse-surface: '#2f312f'
  inverse-on-surface: '#f2f1ee'
  outline: '#6e797a'
  outline-variant: '#bdc9ca'
  surface-tint: '#006970'
  primary: '#006168'
  on-primary: '#ffffff'
  primary-container: '#0d7c84'
  on-primary-container: '#d9fcff'
  inverse-primary: '#7cd4dd'
  secondary: '#45664a'
  on-secondary: '#ffffff'
  secondary-container: '#c3e9c5'
  on-secondary-container: '#496a4e'
  tertiary: '#84481f'
  on-tertiary: '#ffffff'
  tertiary-container: '#a16034'
  on-tertiary-container: '#fff3ed'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#98f1f9'
  primary-fixed-dim: '#7cd4dd'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#c6ecc8'
  secondary-fixed-dim: '#abd0ad'
  on-secondary-fixed: '#01210b'
  on-secondary-fixed-variant: '#2d4e33'
  tertiary-fixed: '#ffdbc8'
  tertiary-fixed-dim: '#ffb68a'
  on-tertiary-fixed: '#321300'
  on-tertiary-fixed-variant: '#6f380f'
  background: '#faf9f6'
  on-background: '#1a1c1a'
  surface-variant: '#e3e2e0'
typography:
  headline-xl:
    fontFamily: Manrope
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  headline-md:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Public Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Public Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Public Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 48px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 64px
  max-width: 1280px
---

## Brand & Style
The design system moves away from clinical sterility toward a "Human-Centric Healthcare" aesthetic. It balances the authority of a medical institution with the empathy of a caregiver. The style is **Modern / Corporate** with a high degree of **Softness**, prioritizing clarity and comfort. 

The goal is to reduce patient anxiety through generous whitespace, soft-edged containers, and a palette that feels organic rather than synthetic. The UI should evoke a sense of "calm competence"—reliable and professional, but fundamentally approachable and warm.

## Colors
The palette is anchored by a deep, warm teal (`#0D7C84`) which provides professional weight without the coldness of standard medical blues. Supporting this is a muted sage green (`#6B8E6F`) for secondary actions and a soft terracotta (`#E89B6A`) for subtle highlights or alerts that require attention without causing alarm.

The foundation of the UI uses "Alabaster" and "Cream" tones instead of pure white to reduce eye strain and provide a more inviting atmosphere. Surfaces should utilize `surface_warm_hex` for containers to create a distinct but soft hierarchy against the `neutral_color_hex` background.

## Typography
The typography strategy pairs **Manrope** for headings with **Public Sans** for body and interface text. Manrope’s modern, slightly rounded geometric forms mirror the "warm professional" brand voice. Public Sans is utilized for its exceptional legibility and institutional trust, ensuring medical data is easily digestible.

High-level headings use a tighter letter-spacing to appear more editorial and authoritative. Body text maintains a generous line height (1.5x) to ensure readability for users who may be under stress or have accessibility requirements.

## Layout & Spacing
The design system utilizes a **Fixed Grid** model for desktop and a **Fluid Grid** for mobile devices. 
- **Desktop:** A 12-column grid with a 1280px max-width, 24px gutters, and 64px side margins.
- **Tablet:** An 8-column grid with 24px gutters and 32px side margins.
- **Mobile:** A 4-column fluid grid with 16px gutters and 16px margins.

Spacing follows a 4px base unit. Internal component padding should default to `md` (16px) for a breathable, open feel. Section-to-section spacing should lean towards `xl` (48px) to reinforce the minimalist, calm aesthetic.

## Elevation & Depth
The system uses **Tonal Layers** combined with **Ambient Shadows** to define hierarchy. Depth is subtle; avoid harsh blacks in shadows. 

Shadows are tinted with the primary teal color at extremely low opacity (e.g., `rgba(13, 124, 132, 0.08)`) to maintain the warm palette. 
- **Level 0 (Background):** `neutral_color_hex`.
- **Level 1 (Cards/Containers):** `surface_warm_hex` with a soft 4px blur shadow.
- **Level 2 (Modals/Popovers):** White surface with a 16px blur, 8px Y-offset shadow.

Avoid "pure" flat design; use 1px inner borders in a slightly darker cream tone to give elements a tangible, high-quality feel.

## Shapes
A **Rounded** shape language (0.5rem base) is applied across the system to remove visual "sharpness" and create a friendlier interface. 
- **Buttons and Inputs:** Use 0.5rem (8px) corners.
- **Cards and Large Containers:** Use 1rem (16px) corners (`rounded-lg`).
- **Contextual Chips:** Use 3rem (full pill) to differentiate them from actionable buttons.

## Components
- **Buttons:** Primary buttons use the deep teal with white text. Secondary buttons use a tonal variant (pale sage background with dark sage text). High-emphasis buttons should have a slight 2px bottom "weight" shadow to appear tactile.
- **Inputs:** Fields use a 1px border in a muted taupe. On focus, the border transitions to the primary teal with a soft outer glow. Use "Public Sans" for all input text to ensure clarity.
- **Cards:** Cards should have a `surface_warm_hex` background and a 1rem corner radius. Padding must be a minimum of 24px to prevent content from feeling crowded.
- **Chips:** Used for medical tags or status. These use the full pill shape. Status colors (Success/Warning) should be adjusted to match the warm theme (e.g., a "Forest Green" instead of "Neon Green").
- **Lists:** Use subtle horizontal dividers in a very light cream/grey. Do not use borders between every item if whitespace can sufficiently separate them.
- **Progress Indicators:** Use the tertiary terracotta color to represent progress, providing a warm, motivating visual cue.