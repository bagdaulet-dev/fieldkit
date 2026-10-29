import {
  CalendarDays,
  Check,
  Download,
  MapPin,
  Package,
  Pencil,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import { useState } from "react";
import { ApiClient } from "../api";
import type { Equipment, Loan, Session } from "../types";
import { categoryNames } from "../types";
import { date, initials } from "../utils";
import { LoanBadge } from "./Collections";
import { GearArt } from "./GearArt";
import { ErrorMessage, Modal } from "./UI";
export function GearDetails({
  item,
  onClose,
  edit,
  checkout,
}: {
  item: Equipment;
  onClose: () => void;
  edit: () => void;
  checkout: () => void;
}) {
  return (
    <Modal title="A closer look." onClose={onClose} className="gear-details">
      <div className={`detail-art category-${item.category.toLowerCase()}`}>
        <GearArt category={item.category} />
        <span>{categoryNames[item.category]}</span>
      </div>
      <div className="detail-heading">
        <h3>{item.name}</h3>
        <p>{item.model || categoryNames[item.category]}</p>
      </div>
      <div className="detail-facts">
        <div>
          <Package size={18} />
          <span>
            On the shelf
            <strong>
              {item.availableQuantity} of {item.totalQuantity} available
            </strong>
          </span>
        </div>
        <div>
          <MapPin size={18} />
          <span>
            Kept at<strong>{item.location || "Not specified"}</strong>
          </span>
        </div>
      </div>
      <p className="detail-description">
        {item.description ||
          "No extra details yet. Add a note about this equipment when you edit it."}
      </p>
      <div className="dialog-actions">
        <button className="button secondary" onClick={edit}>
          <Pencil size={16} />
          Edit details
        </button>
        <button
          className="button primary"
          disabled={item.availableQuantity === 0}
          onClick={checkout}
        >
          Lend this gear
        </button>
      </div>
    </Modal>
  );
}
export function LoanDetails({
  loan,
  onClose,
  returnLoan,
}: {
  loan: Loan;
  onClose: () => void;
  returnLoan: () => void;
}) {
  return (
    <Modal title="The kit’s journey." onClose={onClose} className="loan-details">
      <div className="loan-detail-heading">
        <span className={`loan-detail-art category-${loan.category.toLowerCase()}`}>
          <GearArt category={loan.category} />
        </span>
        <div>
          <h3>{loan.equipmentName}</h3>
          <p>{loan.equipmentModel}</p>
          <LoanBadge loan={loan} />
        </div>
      </div>
      <div className="borrower-detail">
        <span className="avatar large">{initials(loan.borrowerName)}</span>
        <div>
          <span>Borrowed by</span>
          <strong>{loan.borrowerName}</strong>
          <small>{loan.borrowerTeam || "Independent creative"}</small>
        </div>
      </div>
      <div className="detail-facts">
        <div>
          <CalendarDays size={18} />
          <span>
            Checked out<strong>{date(loan.checkedOutAt, true)}</strong>
          </span>
        </div>
        <div>
          <CalendarDays size={18} />
          <span>
            Due back<strong>{date(loan.dueDate, true)}</strong>
          </span>
        </div>
        <div>
          <Package size={18} />
          <span>
            Quantity
            <strong>
              {loan.quantity} {loan.quantity === 1 ? "unit" : "units"}
            </strong>
          </span>
        </div>
        {loan.returnedAt && (
          <div>
            <Check size={18} />
            <span>
              Returned<strong>{date(loan.returnedAt, true)}</strong>
            </span>
          </div>
        )}
      </div>
      <h4 className="detail-notes-title">A little context</h4>
      <p className="detail-description">
        {loan.notes || "No notes were added to this loan."}
      </p>
      <div className="dialog-actions">
        <button className="button secondary" onClick={onClose}>
          Close
        </button>
        {loan.status !== "RETURNED" && (
          <button className="button primary" onClick={returnLoan}>
            <Check size={16} />
            Return gear
          </button>
        )}
      </div>
    </Modal>
  );
}
export function ReturnDialog({
  loan,
  client,
  onClose,
  onSaved,
}: {
  loan: Loan;
  client: ApiClient;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  async function confirm() {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await client.request(`/loans/${loan.id}/return`, { method: "POST", body: {} });
      onSaved();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Welcome back, good gear." onClose={onClose} busy={busy}>
      <p className="dialog-subtitle">
        Confirm that all{" "}
        <strong>
          {loan.quantity} {loan.quantity === 1 ? "unit" : "units"}
        </strong>{" "}
        of <strong>{loan.equipmentName}</strong> have returned from {loan.borrowerName}.
      </p>
      <p className="return-note">
        <Package size={19} />
        The full loan quantity will be added back to available stock.
      </p>
      <ErrorMessage error={error} />
      <div className="dialog-actions">
        <button className="button secondary" disabled={busy} onClick={onClose}>
          Keep on loan
        </button>
        <button className="button primary" disabled={busy} onClick={() => void confirm()}>
          {busy ? "Returning…" : "Confirm return"}
          <Check size={16} />
        </button>
      </div>
    </Modal>
  );
}
export function WorkspaceDialog({
  client,
  session,
  onClose,
  onReset,
  notify,
}: {
  client: ApiClient;
  session: Session;
  onClose: () => void;
  onReset: () => Promise<void>;
  notify: (s: string) => void;
}) {
  const [deleting, setDeleting] = useState(false);
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  async function exportData() {
    setBusy(true);
    setError(null);
    try {
      const data = await client.request("/export");
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }),
      );
      const a = document.createElement("a");
      a.href = url;
      a.download = "fieldkit-workspace.json";
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      notify("Workspace exported.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    setBusy(true);
    setError(null);
    try {
      await client.request("/workspace", { method: "DELETE", body: { confirmation } });
      await onReset();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title="Your little corner of Fieldkit."
      onClose={onClose}
      busy={busy}
      className="workspace-dialog"
    >
      <p className="dialog-subtitle">
        {session.isDemo
          ? "A fictional demo, private to this browser."
          : "A private equipment workspace in this browser."}
      </p>
      <div className="workspace-info">
        <ShieldCheck size={25} />
        <p>
          Your session cookie opens this workspace. Keep it on a trusted device. Sessions
          last up to seven days; export records you want to keep.
        </p>
      </div>
      <button
        className="workspace-option"
        disabled={busy}
        onClick={() => void exportData()}
      >
        <Download size={21} />
        <span>
          <strong>Export your workspace</strong>
          <small>Equipment, people, and complete loan records as JSON</small>
        </span>
      </button>
      <button
        className="workspace-option danger-text"
        disabled={busy}
        onClick={() => setDeleting(true)}
      >
        <Trash2 size={21} />
        <span>
          <strong>Delete workspace</strong>
          <small>Permanently remove its equipment, people, and loans</small>
        </span>
      </button>
      <ErrorMessage error={error} />
      {deleting && (
        <div className="inline-confirm">
          <p>This cannot be undone. Export anything you need first.</p>
          <label>
            Type DELETE to confirm
            <input
              autoComplete="off"
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
            />
          </label>
          <button
            className="button danger"
            disabled={busy || confirmation !== "DELETE"}
            onClick={() => void remove()}
          >
            {busy ? "Deleting…" : "Delete all records"}
          </button>
        </div>
      )}
    </Modal>
  );
}
