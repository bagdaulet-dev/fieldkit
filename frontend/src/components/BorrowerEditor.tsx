import { useState, type FormEvent } from "react";
import { Trash2 } from "lucide-react";
import { ApiClient } from "../api";
import type { Borrower } from "../types";
import { ErrorMessage, Modal } from "./UI";
import { SubmitButton } from "./EditorControls";
export function BorrowerEditor({
  client,
  item,
  onClose,
  onSaved,
}: {
  client: ApiClient;
  item?: Borrower;
  onClose: () => void;
  onSaved: (s: string) => void;
}) {
  const [name, setName] = useState(item?.name || "");
  const [team, setTeam] = useState(item?.team || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [confirm, setConfirm] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await client.request(item ? `/borrowers/${item.id}` : "/borrowers", {
        method: item ? "PUT" : "POST",
        body: { name, team },
      });
      onSaved(item ? "Person updated." : "A new person added.");
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
      await client.request(`/borrowers/${item!.id}`, { method: "DELETE" });
      onSaved("Person removed.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={item ? "A familiar face." : "Who’s making something?"}
      onClose={onClose}
      busy={busy}
    >
      <p className="dialog-subtitle">
        A name and a team are all you need. No login or email required.
      </p>
      <form onSubmit={(e) => void submit(e)}>
        <label>
          Full name
          <input
            name="name"
            autoComplete="off"
            required
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Alex Morgan"
          />
        </label>
        <label>
          Team or club <span>optional</span>
          <input
            name="team"
            autoComplete="off"
            maxLength={100}
            value={team}
            onChange={(e) => setTeam(e.target.value)}
            placeholder="e.g. Film society"
          />
        </label>
        <ErrorMessage error={error} />
        {confirm && (
          <div className="inline-confirm">
            <strong>Remove this person?</strong>
            <p>People with loan history are kept with their records.</p>
            <button
              className="button danger small"
              type="button"
              disabled={busy}
              onClick={() => void remove()}
            >
              Delete person
            </button>
            <button
              className="text-button"
              type="button"
              onClick={() => setConfirm(false)}
            >
              Keep them
            </button>
          </div>
        )}
        <div className="dialog-actions">
          {item && (
            <button
              className="icon-button danger-text"
              type="button"
              disabled={busy}
              aria-label="Remove person"
              onClick={() => setConfirm(true)}
            >
              <Trash2 size={18} />
            </button>
          )}
          <button
            type="button"
            className="button secondary"
            disabled={busy}
            onClick={onClose}
          >
            Cancel
          </button>
          <SubmitButton busy={busy} label={item ? "Save changes" : "Add person"} />
        </div>
      </form>
    </Modal>
  );
}
