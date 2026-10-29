import { ArrowUpRight, LoaderCircle } from "lucide-react";
export function SubmitButton({ busy, label }: { busy: boolean; label: string }) {
  return (
    <button type="submit" className="button primary" disabled={busy}>
      {busy ? (
        <>
          <LoaderCircle size={16} className="spinner" />
          Saving…
        </>
      ) : (
        <>
          {label}
          <ArrowUpRight size={16} />
        </>
      )}
    </button>
  );
}
