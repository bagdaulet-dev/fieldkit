import { useState, type FormEvent } from "react";
import { Trash2 } from "lucide-react";
import { ApiClient } from "../api";
import { categories, categoryNames, type Equipment, type EquipmentInput } from "../types";
import { ErrorMessage, Modal } from "./UI";
import { SubmitButton } from "./EditorControls";
export function EquipmentEditor({
  client,
  item,
  onClose,
  onSaved,
}: {
  client: ApiClient;
  item?: Equipment;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [draft, setDraft] = useState<EquipmentInput>(
    item
      ? {
          name: item.name,
          model: item.model,
          category: item.category,
          location: item.location,
          description: item.description,
          totalQuantity: item.totalQuantity,
        }
      : {
          name: "",
          model: "",
          category: "CAMERAS",
          location: "",
          description: "",
          totalQuantity: 1,
        },
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [confirm, setConfirm] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await client.request(item ? `/equipment/${item.id}` : "/equipment", {
        method: item ? "PUT" : "POST",
        body: draft,
      });
      onSaved(item ? "Equipment updated." : "Equipment added to the shelf.");
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
      await client.request(`/equipment/${item!.id}`, { method: "DELETE" });
      onSaved("Equipment removed.");
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={item ? "Keep the details together." : "A new piece of possibility."}
      onClose={onClose}
      busy={busy}
      className="equipment-editor"
    >
      <p className="dialog-subtitle">
        {item
          ? "Update the equipment and its total stock."
          : "Give your equipment a home in the library."}
      </p>
      <form onSubmit={(e) => void submit(e)} aria-busy={busy}>
        <div className="form-grid">
          <label className="wide">
            Equipment name
            <input
              name="name"
              autoComplete="off"
              maxLength={120}
              required
              value={draft.name}
              placeholder="e.g. Fujifilm X-T5"
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
          </label>
          <label>
            Model or details
            <input
              name="model"
              autoComplete="off"
              maxLength={120}
              value={draft.model}
              placeholder="e.g. Mirrorless camera"
              onChange={(e) => setDraft({ ...draft, model: e.target.value })}
            />
          </label>
          <label>
            Category
            <select
              name="category"
              value={draft.category}
              onChange={(e) =>
                setDraft({ ...draft, category: e.target.value as Equipment["category"] })
              }
            >
              {categories.map((c) => (
                <option value={c} key={c}>
                  {categoryNames[c]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Total quantity
            <input
              name="quantity"
              type="number"
              min={1}
              max={1000}
              step={1}
              required
              value={draft.totalQuantity}
              onChange={(e) =>
                setDraft({ ...draft, totalQuantity: Number(e.target.value) })
              }
            />
          </label>
          <label>
            Storage location
            <input
              name="location"
              autoComplete="off"
              maxLength={120}
              value={draft.location}
              placeholder="e.g. Studio shelf"
              onChange={(e) => setDraft({ ...draft, location: e.target.value })}
            />
          </label>
          <label className="wide">
            Description <span>optional</span>
            <textarea
              name="description"
              rows={3}
              maxLength={2000}
              value={draft.description}
              placeholder="What should someone know about this kit?"
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            />
          </label>
        </div>
        <ErrorMessage error={error} />
        {confirm && (
          <div className="inline-confirm">
            <strong>Remove this equipment?</strong>
            <p>
              Only equipment without loan history can be removed. This action cannot be
              undone.
            </p>
            <button
              className="button danger small"
              type="button"
              disabled={busy}
              onClick={() => void remove()}
            >
              Delete equipment
            </button>
            <button
              className="text-button"
              type="button"
              onClick={() => setConfirm(false)}
            >
              Keep it
            </button>
          </div>
        )}
        <div className="dialog-actions">
          {item && (
            <button
              className="icon-button danger-text"
              type="button"
              disabled={busy}
              aria-label="Remove equipment"
              onClick={() => setConfirm(true)}
            >
              <Trash2 size={18} />
            </button>
          )}
          <button
            className="button secondary"
            type="button"
            disabled={busy}
            onClick={onClose}
          >
            Cancel
          </button>
          <SubmitButton busy={busy} label={item ? "Save changes" : "Add equipment"} />
        </div>
      </form>
    </Modal>
  );
}
