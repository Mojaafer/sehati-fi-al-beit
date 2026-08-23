---
name: Sehati Fi Al-Beit Design System
colors:
  surface: '#f9f9ff'
  surface-dim: '#d3daea'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f0f3ff'
  surface-container: '#e7eefe'
  surface-container-high: '#e2e8f8'
  surface-container-highest: '#dce2f3'
  on-surface: '#151c27'
  on-surface-variant: '#41484d'
  inverse-surface: '#2a313d'
  inverse-on-surface: '#ebf1ff'
  outline: '#71787e'
  outline-variant: '#c0c7ce'
  surface-tint: '#286486'
  primary: '#00344d'
  on-primary: '#ffffff'
  primary-container: '#004c6d'
  on-primary-container: '#85bce2'
  inverse-primary: '#96cdf4'
  secondary: '#006b5e'
  on-secondary: '#ffffff'
  secondary-container: '#7af7e1'
  on-secondary-container: '#007164'
  tertiary: '#4d2600'
  on-tertiary: '#ffffff'
  tertiary-container: '#6e3900'
  on-tertiary-container: '#fba050'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c7e7ff'
  primary-fixed-dim: '#96cdf4'
  on-primary-fixed: '#001e2e'
  on-primary-fixed-variant: '#004c6d'
  secondary-fixed: '#7af7e1'
  secondary-fixed-dim: '#5bdac6'
  on-secondary-fixed: '#00201b'
  on-secondary-fixed-variant: '#005046'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#ffb77d'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#6e3900'
  background: '#f9f9ff'
  on-background: '#151c27'
  surface-variant: '#dce2f3'
typography:
  headline-lg:
    fontFamily: Tajawal
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Tajawal
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Tajawal
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Tajawal
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Tajawal
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Tajawal
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-lg-mobile:
    fontFamily: Tajawal
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-page: 20px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
---

## Brand & Style
The design system is rooted in the "Corporate / Modern" aesthetic, specifically tailored for a Sudanese healthcare context. It prioritizes reliability and professional warmth to build immediate trust with patients in Wad Madani.

The visual narrative focuses on clarity and accessibility. By utilizing a balanced layout with generous white space, the system ensures that critical medical information is easily digestible. The brand personality is "The Empathetic Expert"—authoritative enough to provide medical security, yet soft enough to feel like home-based care. High legibility and a logical information architecture are the core tenets of this system.

## Colors
The palette uses **Medical Blue** as the primary anchor for headers, primary buttons, and structural elements to signify authority and stability. **Trust Green** serves as the secondary color, used for success states, secondary actions, and health-related iconography.

- **Primary (#004C6D):** Deep blue for trust and medical professionalism.
- **Secondary (#00A693):** Clean teal/green for health, vitality, and safety.
- **Tertiary (#F2994A):** A soft amber used sparingly for alerts or "Urgent Service" highlights (as seen in the emergency request banner).
- **Neutrals:** A range of soft greys (from #F9FAFB for backgrounds to #374151 for text) to maintain a high-contrast yet soft-to-the-eye reading experience.

## Typography
This design system utilizes **Tajawal** (mapped here to professional equivalents) for its exceptional Arabic legibility and clean geometric structure. 

The type hierarchy is designed for right-to-left (RTL) reading patterns. Headlines use a heavier weight to anchor the eye on section starts, while body text maintains a generous line height (1.5x) to ensure comfortable reading for all age groups, particularly for medical instructions or service descriptions.

## Layout & Spacing
The layout follows a **fluid grid** model optimized for mobile-first healthcare delivery. 

- **Safe Areas:** A 20px horizontal margin is maintained on all screens to prevent content from touching device edges.
- **Guttering:** 16px spacing between cards in a list or grid view.
- **Vertical Rhythm:** Elements are stacked using an 8px base unit. 16px is the standard separation between functional groups, while 24px separates distinct sections (e.g., separating "Health Services" from "Available Now").
- **Alignment:** Content is strictly RTL-aligned, with icons typically leading on the right and chevron indicators on the left.

## Elevation & Depth
Depth is conveyed through **Tonal Layers** and **Ambient Shadows** to create a clear sense of tappable surfaces.

- **Surface Levels:** The base background is light grey (#F9FAFB). Interactive cards sit on a pure white (#FFFFFF) surface.
- **Shadows:** Use extremely soft, diffused shadows (0px 4px 12px rgba(0, 0, 0, 0.05)). Shadows should not feel "heavy"—they are merely a subtle lift to indicate that a card is a separate interactive entity.
- **Active State:** When a card is selected, use a 2px border in the Primary Blue instead of increasing shadow depth, maintaining a clean medical look.

## Shapes
The design system employs **Rounded** geometry to evoke a sense of "caring" and "safety." 

- **Standard Containers:** Cards and input fields use a 12px (0.75rem) radius.
- **Large Components:** Hero banners and main action containers use 16px (1rem) for a friendlier, modern appearance.
- **Buttons:** Primary call-to-action buttons use a 12px radius to match input fields, creating a unified, professional block language.
- **Status Pills:** Small labels (e.g., "Available Now") use a fully pill-shaped (rounded-full) radius to distinguish them from interactive buttons.

## Components
Consistent implementation of these components ensures the app remains accessible and professional:

- **Primary Buttons:** High-contrast Medical Blue background with white text. Height is fixed at 48px or 56px for easy thumb-tapping.
- **Input Fields:** 12px rounded corners with a 1px border (#E5E7EB). Labels sit above the field in a bold `label-md` style.
- **Service Cards:** White background, 12px radius, subtle shadow. Icons should be centered within a soft-tinted circular background (e.g., Light Blue or Light Green).
- **Status Indicators:** Use small circular dots or pill-shaped chips. Green for "Available," Amber for "Pending," and Grey for "Inactive."
- **List Items:** Use a 1px bottom border for separation within lists, with a 16px padding to ensure touch targets are sufficient for elderly users.
- **Action Sheets:** Pull-up menus from the bottom for booking confirmations, using a 24px top-corner radius to differentiate from standard cards.