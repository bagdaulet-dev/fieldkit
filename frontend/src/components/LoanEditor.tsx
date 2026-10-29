import { useEffect, useState, type FormEvent } from "react";
import { ApiClient } from "../api";
import type { Equipment, Borrower, Loan, Page } from "../types";
import { afterDays, today } from "../utils";
import { Empty, ErrorMessage, Modal } from "./UI";
import { SubmitButton } from "./EditorControls";
async function options<T>(client: ApiClient, path: string, signal: AbortSignal) {
  const items: T[] = [];
  for (let page = 1; page <= 20; page++) {
    const result = await client.request<Page<T>>(`${path}?pageSize=50&page=${page}`, {
      signal,
    });
    items.push(...result.items);
    if (page >= result.totalPages) return items;
  }
  throw new Error(
    "The workspace selection limit was reached. Search your records from the library.",
  );
}
export function LoanEditor({
  client,
  item,
  onClose,
  onSaved,
  setup,
}: {
  client: ApiClient;
  item?: Equipment;
  onClose: () => void;
  onSaved: (s: string) => void;
  setup: (view: "equipment" | "people") => void;
}) {
  const [gear, setGear] = useState<Equipment[]>([]);
  const [people, setPeople] = useState<Borrower[]>([]);
  const [equipmentId, setEquipmentId] = useState(item?.id || "");
  const [borrowerId, setBorrowerId] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [dueDate, setDueDate] = useState(afterDays(7));
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      options<Equipment>(client, "/equipment", controller.signal),
      options<Borrower>(client, "/borrowers", controller.signal),
    ])
      .then(([gear, people]) => {
        setGear(gear);
        setPeople(people);
        if (!item) setEquipmentId(gear.find((g) => g.availableQuantity > 0)?.id || "");
        setBorrowerId(people[0]?.id || "");
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(e);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [client, item]);
  const selected = gear.find((g) => g.id === equipmentId);
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await client.request<Loan>("/loans", {
        method: "POST",
        body: { equipmentId, borrowerId, quantity, dueDate, notes },
      });
      onSaved("Gear checked out. Good things ahead.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title="Send a little possibility out."
      onClose={onClose}
      busy={busy}
      className="loan-editor"
    >
      <p className="dialog-subtitle">
        One checkout. One person. Something good in the making.
      </p>
      <ErrorMessage error={error} />
      {loading ? (
        <p className="loading-message" role="status">
          Checking the shelf…
        </p>
      ) : !gear.length || !people.length ? (
        <Empty
          title="Start with the essentials."
          description="Add equipment and a person before creating a loan."
        >
          <button
            className="button primary"
            onClick={() => setup(!gear.length ? "equipment" : "people")}
          >
            {!gear.length ? "Add equipment" : "Add person"}
          </button>
        </Empty>
      ) : (
        <form onSubmit={(e) => void submit(e)} aria-busy={busy}>
          <label>
            Equipment
            <select
              name="equipment"
              required
              value={equipmentId}
              onChange={(e) => {
                setEquipmentId(e.target.value);
                setQuantity(1);
              }}
            >
              <option value="" disabled>
                Choose a piece of kit
              </option>
              {gear.map((g) => (
                <option key={g.id} value={g.id} disabled={g.availableQuantity === 0}>
                  {g.name} · {g.availableQuantity} available
                </option>
              ))}
            </select>
          </label>
          {selected && (
            <p className="availability-hint">
              {selected.availableQuantity} of {selected.totalQuantity} units available
              {selected.location ? ` · ${selected.location}` : ""}
            </p>
          )}
          <label>
            Borrowed by
            <select
              name="borrower"
              required
              value={borrowerId}
              onChange={(e) => setBorrowerId(e.target.value)}
            >
              <option value="" disabled>
                Choose a person
              </option>
              {people.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                  {p.team ? ` · ${p.team}` : ""}
                </option>
              ))}
            </select>
          </label>
          <div className="form-grid">
            <label>
              Quantity
              <input
                name="quantity"
                type="number"
                min={1}
                max={selected?.availableQuantity || 1}
                step={1}
                required
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
              />
            </label>
            <label>
              Due back
              <input
                name="dueDate"
                type="date"
                required
                min={today()}
                max={afterDays(365)}
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </label>
          </div>
          <label>
            Notes <span>optional</span>
            <textarea
              name="notes"
              maxLength={2000}
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="A project name, a reminder, or a little context…"
            />
          </label>
          <p className="form-note">
            The whole quantity is returned together. Availability is checked again when
            you save.
          </p>
          <div className="dialog-actions">
            <button
              className="button secondary"
              type="button"
              disabled={busy}
              onClick={onClose}
            >
              Cancel
            </button>
            <SubmitButton busy={busy} label="Create loan" />
          </div>
        </form>
      )}
    </Modal>
  );
}
