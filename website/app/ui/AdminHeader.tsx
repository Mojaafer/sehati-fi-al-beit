import type { ReactNode } from "react";

type Props = {
  eyebrow: string;
  title: string;
  description?: string;
  /** Rendered above the eyebrow (e.g. the back link some screens carry). */
  before?: ReactNode;
  /**
   * Rendered as-is after the title block: callers keep their own wrapper (or
   * lack of one), because the four admin screens disagree on it and normalising
   * that here would shift alignment.
   */
  actions?: ReactNode;
};

/**
 * The admin section header: eyebrow kicker, title, description, optional
 * actions. Copy-pasted across the four admin list screens with only the words
 * changing; the DOM below is byte-identical to what each screen had before.
 */
export function AdminHeader({ eyebrow, title, description, before, actions }: Props) {
  return (
    <header className="admin-header">
      <div>
        {before}
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {actions}
    </header>
  );
}
