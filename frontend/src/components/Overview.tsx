import { ArrowUpRight, Check, ChevronRight, PackageCheck, Plus } from "lucide-react";
import { categoryNames, type Dashboard, type Equipment, type Loan } from "../types";
import { number } from "../utils";
import { GearCard, LoanTable } from "./Collections";
import { GearShelf } from "./GearArt";
import { Empty } from "./UI";
export function Overview({
  data,
  add,
  addLoan,
  browse,
  openGear,
  editGear,
  checkout,
  openLoan,
  returnLoan,
  explore,
  demoAvailable,
  busy,
}: {
  data: Dashboard | null;
  add: () => void;
  addLoan: () => void;
  browse: (view: "equipment" | "loans", status?: string) => void;
  openGear: (e: Equipment) => void;
  editGear: (e: Equipment) => void;
  checkout: (e: Equipment) => void;
  openLoan: (l: Loan) => void;
  returnLoan: (l: Loan) => void;
  explore: () => void;
  demoAvailable: boolean;
  busy: boolean;
}) {
  const total = data?.totalUnits ?? 0,
    available = data?.availableUnits ?? 0;
  const out = total - available;
  const percent = total ? out / total : 0;
  return (
    <>
      <section className="overview-hero">
        <div className="hero-copy">
          <p className="hero-intro">
            <span className="tiny-spark">✳</span>For the things you make together.
          </p>
          <h1>
            Good gear.
            <br />
            In good hands.
          </h1>
          <p>
            Keep track of the kit.
            <br />
            Make more room for the making.
          </p>
          <div className="hero-buttons">
            <button className="button primary" onClick={addLoan}>
              Lend something good
              <ArrowUpRight size={17} />
            </button>
            <button className="hero-library-link" onClick={() => browse("equipment")}>
              Explore the library
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
        <GearShelf />
      </section>
      <section className="overview-summary" aria-label="Workspace stock summary">
        <button onClick={() => browse("equipment")}>
          <strong>{number(data?.equipmentTypes ?? 0)}</strong>
          <span>
            pieces of possibility<small>Equipment types in your library</small>
          </span>
        </button>
        <button onClick={() => browse("equipment", "AVAILABLE")}>
          <strong>{number(available)}</strong>
          <span>
            ready to go<small>Units available on the shelf</small>
          </span>
        </button>
        <button onClick={() => browse("loans", "ACTIVE")}>
          <strong>{number(data?.activeLoans ?? 0)}</strong>
          <span>
            out making things<small>Active loans, all in one place</small>
          </span>
        </button>
        <button
          className={(data?.overdueLoans ?? 0) > 0 ? "has-overdue" : ""}
          onClick={() => browse("loans", "OVERDUE")}
        >
          <strong>{number(data?.overdueLoans ?? 0)}</strong>
          <span>
            need a little nudge<small>Loans past their due date</small>
          </span>
        </button>
      </section>
      {data && total === 0 && demoAvailable && (
        <div className="demo-invitation">
          <span className="demo-invite-icon">
            <PackageCheck size={22} />
          </span>
          <div>
            <strong>Take Fieldkit for a little spin.</strong>
            <p>Explore a private workspace with fictional gear, people, and loans.</p>
          </div>
          <button className="button secondary" disabled={busy} onClick={explore}>
            {busy ? "Opening…" : "Explore the demo"}
            <ArrowUpRight size={16} />
          </button>
        </div>
      )}
      <section className="overview-grid">
        <div className="overview-loans">
          <div className="section-heading">
            <div>
              <h2>Out making things</h2>
              <p>Every loan, a project in progress.</p>
            </div>
            <button className="text-button" onClick={() => browse("loans", "ACTIVE")}>
              All loans
              <ChevronRight size={16} />
            </button>
          </div>
          {data?.recentLoans.length ? (
            <LoanTable items={data.recentLoans} open={openLoan} returnLoan={returnLoan} />
          ) : (
            <Empty
              title="Your shelf is ready."
              description="Check out some gear and its journey will appear here."
            >
              <button className="button secondary" onClick={addLoan}>
                <Plus size={16} />
                Create a loan
              </button>
            </Empty>
          )}
        </div>
        <aside className="shelf-status">
          <div className="section-heading">
            <h2>A well-kept shelf</h2>
            <span className="shelf-status-icon">
              <Check size={17} />
            </span>
          </div>
          <div className="stock-ring">
            <svg viewBox="0 0 180 180" aria-hidden="true">
              <circle cx="90" cy="90" r="72" className="ring-track" />
              <circle
                cx="90"
                cy="90"
                r="72"
                className="ring-value"
                pathLength="100"
                strokeDasharray={`${percent * 100} 100`}
                transform="rotate(-90 90 90)"
              />
            </svg>
            <div>
              <strong>{total ? Math.round((available / total) * 100) : 0}%</strong>
              <span>available</span>
            </div>
          </div>
          <div className="shelf-key">
            <span>
              <i />
              {available} on the shelf
            </span>
            <span>
              <i />
              {out} on loan
            </span>
          </div>
          <div className="category-breakdown">
            {data?.categories.length ? (
              data.categories.map((row) => (
                <div key={row.category}>
                  <span>{categoryNames[row.category]}</span>
                  <strong>
                    {row.available}
                    <small>/{row.total}</small>
                  </strong>
                </div>
              ))
            ) : (
              <p>Your inventory overview will appear as you add equipment.</p>
            )}
          </div>
        </aside>
      </section>
      <section className="featured-section">
        <div className="section-heading">
          <div>
            <h2>A few favorites from the shelf</h2>
            <p>For whatever you’re making next.</p>
          </div>
          <button className="text-button" onClick={add}>
            <Plus size={16} />
            Add equipment
          </button>
        </div>
        {data?.featuredEquipment.length ? (
          <div className="featured-grid">
            {data.featuredEquipment.map((item) => (
              <GearCard
                key={item.id}
                item={item}
                open={openGear}
                edit={editGear}
                checkout={checkout}
              />
            ))}
          </div>
        ) : (
          <div className="add-gear-placeholder">
            <Plus size={24} />
            <div>
              <h3>Start with your first piece of kit.</h3>
              <p>A camera, a laptop, a light. Give it a place in your library.</p>
            </div>
            <button className="button secondary" onClick={add}>
              Add equipment
            </button>
          </div>
        )}
      </section>
    </>
  );
}
