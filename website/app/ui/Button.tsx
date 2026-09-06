import type { ButtonHTMLAttributes, ReactNode } from "react";

export type ButtonVariant = "primary" | "secondary" | "light";

const variantClass: Record<ButtonVariant, string> = {
  primary: "primary-button",
  secondary: "secondary-button",
  light: "light-button",
};

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  children: ReactNode;
};

/**
 * The shared call-to-action. The three visual variants used to be retyped as
 * string literals (`"primary-button"`, …) at every call site, so a rename or a
 * fourth variant meant editing a dozen files. Extra classes (e.g. `"full"`)
 * append after the variant class, preserving existing overrides.
 */
export function Button({ variant = "primary", className, children, ...props }: Props) {
  const merged = className ? `${variantClass[variant]} ${className}` : variantClass[variant];
  return (
    <button type="button" className={merged} {...props}>
      {children}
    </button>
  );
}
