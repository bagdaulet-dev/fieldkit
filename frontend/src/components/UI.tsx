import { useEffect, useId, useRef, type ReactNode } from "react";
import { AlertCircle, Check, ChevronLeft, ChevronRight, X } from "lucide-react";
import type { Page } from "../types";
export function Brand() {
  return (
    <span className="brand" translate="no">
      <span className="brand-mark" aria-hidden="true">
        <i />
        <i />
        <i />
        <i />
      </span>
      fieldkit<span className="brand-period">.</span>
    </span>
  );
}
export function Modal({
  title,
  onClose,
  busy = false,
  children,
  className = "",
}: {
  title: string;
  onClose: () => void;
  busy?: boolean;
  children: ReactNode;
  className?: string;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    const previous = document.activeElement;
    dialog?.showModal();
    return () => {
      dialog?.close();
      if (
        previous instanceof HTMLElement &&
        previous.isConnected &&
        !previous.matches(":disabled")
      )
        previous.focus({ preventScroll: true });
      else document.getElementById("main-content")?.focus({ preventScroll: true });
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={`dialog ${className}`}
      aria-labelledby={id}
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="dialog-heading">
        <h2 id={id}>{title}</h2>
        <button
          className="icon-button"
          type="button"
          aria-label="Close dialog"
          disabled={busy}
          onClick={onClose}
        >
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  );
}
export function ErrorMessage({ error }: { error: unknown }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error) ref.current?.focus();
  }, [error]);
  if (!error) return null;
  return (
    <div ref={ref} role="alert" tabIndex={-1} className="error-message">
      <AlertCircle size={17} />
      <span>{error instanceof Error ? error.message : String(error)}</span>
    </div>
  );
}
export function Toast({ message, close }: { message: string; close: () => void }) {
  useEffect(() => {
    const timer = setTimeout(close, 4500);
    return () => clearTimeout(timer);
  }, [message, close]);
  return (
    <div role="status" className="toast">
      <Check size={18} />
      {message}
      <button className="icon-button" aria-label="Dismiss notification" onClick={close}>
        <X size={15} />
      </button>
    </div>
  );
}
export function Empty({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <span className="empty-shape" aria-hidden="true" />
      <h3>{title}</h3>
      <p>{description}</p>
      {children}
    </div>
  );
}
export function Pagination({
  page,
  setPage,
}: {
  page: Page<unknown>;
  setPage: (n: number) => void;
}) {
  return (
    <div className="pagination">
      <span>
        {page.totalItems === 0
          ? "No records"
          : `${(page.page - 1) * page.pageSize + 1}–${Math.min(page.page * page.pageSize, page.totalItems)} of ${page.totalItems}`}
      </span>
      <div>
        <button
          className="icon-button"
          aria-label="Previous page"
          disabled={page.page <= 1}
          onClick={() => setPage(page.page - 1)}
        >
          <ChevronLeft size={17} />
        </button>
        <span>
          Page {page.page} of {Math.max(1, page.totalPages)}
        </span>
        <button
          className="icon-button"
          aria-label="Next page"
          disabled={page.page >= page.totalPages}
          onClick={() => setPage(page.page + 1)}
        >
          <ChevronRight size={17} />
        </button>
      </div>
    </div>
  );
}
