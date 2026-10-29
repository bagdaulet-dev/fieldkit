import { useCallback, useEffect, useState } from "react";
import { Box, ChevronDown, Heart, LockKeyhole, Menu, Settings2, X } from "lucide-react";
import { useFieldkit, useWorkspace } from "./data";
import type { Borrower, Equipment, Filters, Loan, View } from "./types";
import { Brand, ErrorMessage, Toast } from "./components/UI";
import { Overview } from "./components/Overview";
import { EquipmentView, LoansView, PeopleView } from "./components/Collections";
import { BorrowerEditor } from "./components/BorrowerEditor";
import { EquipmentEditor } from "./components/EquipmentEditor";
import { LoanEditor } from "./components/LoanEditor";
import {
  GearDetails,
  LoanDetails,
  ReturnDialog,
  WorkspaceDialog,
} from "./components/Details";
const titles: Record<View, string> = {
  overview: "Overview",
  equipment: "Gear library",
  loans: "Loans",
  people: "People",
};
function currentView(): View {
  const value = new URL(location.href).searchParams.get("view");
  return value === "equipment" || value === "loans" || value === "people"
    ? value
    : "overview";
}
export default function App() {
  const app = useFieldkit();
  const [view, setView] = useState<View>(currentView);
  const [filters, setFilters] = useState<Filters>({
    search: "",
    category: "",
    status: "ALL",
    page: 1,
  });
  const [menu, setMenu] = useState(false);
  const [gearEditor, setGearEditor] = useState<Equipment | "new" | null>(null);
  const [personEditor, setPersonEditor] = useState<Borrower | "new" | null>(null);
  const [loanEditor, setLoanEditor] = useState<Equipment | "new" | null>(null);
  const [gearDetail, setGearDetail] = useState<Equipment | null>(null);
  const [loanDetail, setLoanDetail] = useState<Loan | null>(null);
  const [returning, setReturning] = useState<Loan | null>(null);
  const [settings, setSettings] = useState(false);
  const [toast, setToast] = useState("");
  const [actionError, setActionError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const closeToast = useCallback(() => setToast(""), []);
  const data = useWorkspace(app.client, app.session, view, filters, app.version);
  function navigate(next: View, status = "ALL") {
    setView(next);
    setFilters({ search: "", category: "", status, page: 1 });
    setMenu(false);
    const url = new URL(location.href);
    url.searchParams.set("view", next);
    history.pushState(null, "", url);
  }
  useEffect(() => {
    const pop = () => {
      setView(currentView());
      setFilters({ search: "", category: "", status: "ALL", page: 1 });
      setMenu(false);
    };
    window.addEventListener("popstate", pop);
    return () => window.removeEventListener("popstate", pop);
  }, []);
  function saved(message: string) {
    setGearEditor(null);
    setPersonEditor(null);
    setLoanEditor(null);
    setGearDetail(null);
    setLoanDetail(null);
    setFilters((f) => ({ ...f, page: 1 }));
    app.refresh();
    setToast(message);
  }
  async function explore() {
    setBusy(true);
    setActionError(null);
    try {
      const session = await app.client.request<typeof app.session>("/demo", {
        method: "POST",
        body: {},
      });
      if (session) {
        app.client.csrf = session.csrfToken;
        app.setSession(session);
        app.refresh();
        document.getElementById("main-content")?.focus({ preventScroll: true });
      }
    } catch (e) {
      setActionError(e);
    } finally {
      setBusy(false);
    }
  }
  if (app.loading)
    return (
      <main className="boot-screen">
        <Brand />
        <span className="loading-dots" />
        <p>Opening your little gear room…</p>
      </main>
    );
  if (!app.session)
    return (
      <main className="boot-screen">
        <Brand />
        <h1>Your workspace is taking a moment.</h1>
        <ErrorMessage error={app.error} />
        <button className="button primary" onClick={() => void app.connect()}>
          Reconnect
        </button>
      </main>
    );
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to workspace
      </a>
      <header className="site-header">
        <div className="header-inner">
          <a
            className="brand-link"
            href="/"
            onClick={(e) => {
              if (!e.metaKey && !e.ctrlKey) {
                e.preventDefault();
                navigate("overview");
              }
            }}
          >
            <Brand />
          </a>
          <button
            className="mobile-toggle icon-button"
            aria-label={menu ? "Close navigation" : "Open navigation"}
            aria-expanded={menu}
            onClick={() => setMenu(!menu)}
          >
            {menu ? <X /> : <Menu />}
          </button>
          <nav className={menu ? "open" : ""} aria-label="Workspace navigation">
            {(Object.keys(titles) as View[]).map((name) => (
              <a
                key={name}
                href={`/?view=${name}`}
                aria-label={titles[name]}
                aria-current={view === name ? "page" : undefined}
                className={view === name ? "active" : ""}
                onClick={(e) => {
                  if (!e.metaKey && !e.ctrlKey) {
                    e.preventDefault();
                    navigate(name);
                  }
                }}
              >
                {titles[name]}
                {name === "loans" && (data.dashboard?.activeLoans ?? 0) > 0 && (
                  <span>{data.dashboard?.activeLoans}</span>
                )}
              </a>
            ))}
          </nav>
          <button
            className="workspace-switch"
            aria-label="Open workspace settings"
            onClick={() => setSettings(true)}
          >
            <span className="workspace-icon">
              <Box size={18} />
            </span>
            <span>
              Creative studio
              <small>
                {app.session.isDemo ? "Demo workspace" : "Personal workspace"}
              </small>
            </span>
            <ChevronDown size={15} />
          </button>
        </div>
      </header>
      <main id="main-content" className="main-content" tabIndex={-1}>
        <div className="workspace-meta">
          <span>
            <span className="meta-dot" />
            {app.session.isDemo
              ? "A little demo. All yours."
              : "Your equipment workspace"}
          </span>
          <span>
            <LockKeyhole size={12} />
            Private to this browser
          </span>
        </div>
        <ErrorMessage error={app.error || data.error || actionError} />
        {view === "overview" ? (
          <Overview
            data={data.dashboard}
            add={() => setGearEditor("new")}
            addLoan={() => setLoanEditor("new")}
            browse={navigate}
            openGear={setGearDetail}
            editGear={setGearEditor}
            checkout={setLoanEditor}
            openLoan={setLoanDetail}
            returnLoan={setReturning}
            explore={() => void explore()}
            demoAvailable={app.session.demoEnabled}
            busy={busy}
          />
        ) : view === "equipment" ? (
          <EquipmentView
            page={data.equipment}
            filters={filters}
            setFilters={(f) => setFilters((p) => ({ ...p, ...f }))}
            add={() => setGearEditor("new")}
            open={setGearDetail}
            edit={setGearEditor}
            checkout={setLoanEditor}
            loading={data.loading}
          />
        ) : view === "loans" ? (
          <LoansView
            page={data.loans}
            filters={filters}
            setFilters={(f) => setFilters((p) => ({ ...p, ...f }))}
            add={() => setLoanEditor("new")}
            open={setLoanDetail}
            returnLoan={setReturning}
            loading={data.loading}
          />
        ) : (
          <PeopleView
            page={data.people}
            filters={filters}
            setFilters={(f) => setFilters((p) => ({ ...p, ...f }))}
            add={() => setPersonEditor("new")}
            edit={setPersonEditor}
            loading={data.loading}
          />
        )}
        <footer className="site-footer">
          <Brand />
          <span>
            Made for sharing. Kept together.
            <Heart size={13} />
          </span>
          <button className="text-button" onClick={() => setSettings(true)}>
            <Settings2 size={14} />
            Your workspace
          </button>
        </footer>
      </main>
      {gearEditor && (
        <EquipmentEditor
          client={app.client}
          item={gearEditor === "new" ? undefined : gearEditor}
          onClose={() => setGearEditor(null)}
          onSaved={saved}
        />
      )}{" "}
      {personEditor && (
        <BorrowerEditor
          client={app.client}
          item={personEditor === "new" ? undefined : personEditor}
          onClose={() => setPersonEditor(null)}
          onSaved={saved}
        />
      )}{" "}
      {loanEditor && (
        <LoanEditor
          client={app.client}
          item={loanEditor === "new" ? undefined : loanEditor}
          onClose={() => setLoanEditor(null)}
          onSaved={saved}
          setup={(next) => {
            setLoanEditor(null);
            navigate(next);
            if (next === "equipment") setGearEditor("new");
            else setPersonEditor("new");
          }}
        />
      )}
      {gearDetail && !gearEditor && !loanEditor && (
        <GearDetails
          item={gearDetail}
          onClose={() => setGearDetail(null)}
          edit={() => setGearEditor(gearDetail)}
          checkout={() => setLoanEditor(gearDetail)}
        />
      )}{" "}
      {loanDetail && !returning && (
        <LoanDetails
          loan={loanDetail}
          onClose={() => setLoanDetail(null)}
          returnLoan={() => setReturning(loanDetail)}
        />
      )}{" "}
      {returning && (
        <ReturnDialog
          loan={returning}
          client={app.client}
          onClose={() => setReturning(null)}
          onSaved={() => {
            setReturning(null);
            setLoanDetail(null);
            app.refresh();
            setToast("Gear returned. Back on the shelf.");
          }}
        />
      )}
      {settings && (
        <WorkspaceDialog
          client={app.client}
          session={app.session}
          onClose={() => setSettings(false)}
          notify={setToast}
          onReset={async () => {
            await app.connect();
            setSettings(false);
            navigate("overview");
            app.refresh();
            setToast("A fresh workspace is ready.");
          }}
        />
      )}
      {toast && <Toast message={toast} close={closeToast} />}
    </div>
  );
}
