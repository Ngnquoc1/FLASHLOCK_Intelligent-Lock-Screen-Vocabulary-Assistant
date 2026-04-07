# The Design System: Educational Elegance & Tonal Depth



This design system is engineered to transform a standard educational utility into a premium, immersive "Digital Sanctuary" for language acquisition. By leaning into the deep, nocturnal LUNA palette, we create a high-contrast, editorial experience that focuses the user’s cognitive load entirely on the content.



---



### 1. Overview & Creative North Star: "The Luminescent Scholar"

The Creative North Star for this system is **The Luminescent Scholar**. We move away from the "toy-like" aesthetic of common learning apps, instead opting for a sophisticated, high-end editorial feel.



**The Editorial Shift:**

- **Intentional Asymmetry:** Avoid perfectly centered grids. Use the `spacing-8` and `spacing-12` tokens to create "breathing rooms" that push content into unexpected, purposeful positions.

- **Overlapping Planes:** Treat learning cards not as flat boxes, but as floating shards of information that subtly overlap background elements.

- **Typography Scale:** We use a dramatic contrast between `display-lg` for progress milestones and `body-sm` for technical metadata, creating a clear hierarchy of importance.



---



### 2. Colors: Depth over Definition

Our palette is rooted in the deep sea and night sky, designed to reduce eye strain during long study sessions.



**The "No-Line" Rule:**

Standard 1px borders are strictly prohibited for sectioning. Separation must be achieved through background shifts.

- Place a `surface-container-low` (#001b3f) element against a `surface` (#00132f) background to define boundaries.



**Surface Hierarchy & Nesting:**

Use the Material-based container tiers to create a physical sense of "stacking":

- **Base Layer:** `surface` (#00132f)

- **Primary Content Areas:** `surface-container` (#041f43)

- **Elevated Interactions:** `surface-container-high` (#122a4e) or `highest` (#1e3559)



**The "Glass & Gradient" Rule:**

To provide "soul" to the UI, use a subtle linear gradient on main CTAs:

`Linear Gradient: primary (#94cdf9) to primary-container (#26658c) at 135°.`

For floating navigation or lock-screen overlays, use **Glassmorphism**: `surface-variant` (#1e3559) at 80% opacity with a `20px` backdrop blur.



---



### 3. Typography: The Intellectual Voice

We pair two distinct sans-serifs to balance authority with approachability.



* **Headlines (Manrope):** Chosen for its geometric modernism. Use `headline-lg` for new word introductions to create a "Hero" moment for the vocabulary.

* **Body (Be Vietnam Pro):** A highly legible typeface for definitions and examples. Use `body-md` for standard text and `label-sm` for "Heard/Remembered" metadata.

* **The Hierarchy:** Use `display-md` for "Words Mastered" counters to celebrate progress with scale. Title-level typography should always use `on-surface` (#d7e3ff) for maximum contrast.



---



### 4. Elevation & Depth: Tonal Layering

We reject drop shadows in favor of **Tonal Layering**. Depth is a result of color value, not artificial lighting.



- **The Layering Principle:** Place a `surface-container-lowest` (#000e26) card inside a `surface-container-low` (#001b3f) section to create a "sunken" or "embedded" feel for secondary information.

- **Ambient Shadows:** If a card must float (e.g., a word card during a flip animation), use a diffused shadow: `box-shadow: 0 12px 32px rgba(0, 19, 47, 0.4);`. The shadow must be a tinted version of the background, never pure black.

- **The "Ghost Border":** For interactive inputs, use the `outline-variant` (#41484e) at **15% opacity**. This creates a suggestion of a border without breaking the fluid, organic feel of the UI.



---



### 5. Components: Fluid Primitives



**Buttons:**

- **Primary:** Gradient-filled (`primary` to `primary-container`) with `rounded-xl` (1.5rem) corners. No border.

- **Secondary:** `surface-container-highest` background with `primary` (#94cdf9) text.

- **Tertiary:** Transparent background, `label-md` uppercase text with `spacing-1.5` letter spacing for an editorial feel.



**Learning Cards (The Core Component):**

- **Structure:** Use `rounded-xl` (1.5rem). Never use dividers.

- **Separation:** Separate the "Word" from the "Definition" using a `spacing-6` vertical gap.

- **States:** A "Remembered" state should shift the background to a subtle `secondary-container` (#007587) tint.



**The "Action Cluster" (Heard, Remembered, Forgot):**

- Forgo standard buttons. Use large, soft-touch `rounded-lg` tiles.

- **Heard:** `surface-container-high` with `secondary` icon.

- **Remembered:** `primary-container` with `on-primary-container` icon.

- **Forgot:** `surface-container-low` with `outline` icon.



**Input Fields:**

- Use a "Flushed" style. No box; only a `surface-container-highest` background with a `rounded-sm` bottom-only accent.



---



### 6. Do's and Don'ts



**Do:**

- **Do** use `spacing-10` and `spacing-16` for "hero" margins to create a premium, uncrowded feel.

- **Do** use `secondary_fixed_dim` (#7dd3e7) for success states; it’s more sophisticated than a standard "Success Green."

- **Do** leverage the `full` (9999px) roundedness for progress bars to make the learning journey feel smooth and continuous.



**Don't:**

- **Don't** use 1px dividers. If content needs to be separated, use a 24px (spacing-6) vertical void or a tonal shift.

- **Don't** use pure white (#FFFFFF). All "white" text should be `on-surface` (#d7e3ff) to maintain the LUNA atmosphere.

- **Don't** use standard "Material Blue." Stick strictly to the `primary` (#94cdf9) and `accent` (#023859) tokens to preserve the custom brand identity.