import {
  ArrowUpRight,
  Check,
  ChevronRight,
  MoreHorizontal,
  Plus,
  Search,
  SlidersHorizontal,
} from "lucide-react";
import {
  categories,
  categoryNames,
  type Borrower,
  type Equipment,
  type Filters,
  type Loan,
  type Page,
} from "../types";
import { date, initials } from "../utils";
import { GearArt } from "./GearArt";
import { Empty, Pagination } from "./UI";
export function GearCard({
  item,
  open,
  edit,
  checkout,
}: {
  item: Equipment;
  open: (e: Equipment) => void;
  edit: (e: Equipment) => void;
  checkout: (e: Equipment) => void;
}) {
  return (
    <article className={`gear-card category-${item.category.toLowerCase()}`}>
      <div className="gear-picture">
        <span className="gear-category">{categoryNames[item.category]}</span>
        <button
          className="icon-button gear-more"
          aria-label={`Edit ${item.name}`}
          onClick={() => edit(item)}
        >
          <MoreHorizontal size={20} />
        </button>
        <button
          className="gear-art-button"
          aria-label={`View ${item.name}`}
          onClick={() => open(item)}
        >
          <GearArt category={item.category} />
        </button>
      </div>
      <div className="gear-card-copy">
        <button className="gear-title" onClick={() => open(item)}>
          {item.name}
        </button>
        <p>{item.model || categoryNames[item.category]}</p>
        <div className="gear-card-bottom">
          <span
            className={`stock-label ${item.availableQuantity === 0 ? "stock-out" : ""}`}
          >
            <span />
            {item.availableQuantity} of {item.totalQuantity} available
          </span>
          <button
            className="gear-borrow"
            disabled={item.availableQuantity === 0}
            aria-label={`Lend ${item.name}`}
            onClick={() => checkout(item)}
          >
            <ArrowUpRight size={19} />
          </button>
        </div>
      </div>
    </article>
  );
}
export function LoanBadge({ loan }: { loan: Loan }) {
  return (
    <span className={`loan-badge ${loan.status.toLowerCase()}`}>
      <span />
      {loan.status === "OVERDUE"
        ? "Overdue"
        : loan.status === "RETURNED"
          ? "Returned"
          : "On loan"}
    </span>
  );
}
export function LoanTable({
  items,
  open,
  returnLoan,
}: {
  items: Loan[];
  open: (l: Loan) => void;
  returnLoan: (l: Loan) => void;
}) {
  return (
    <div className="table-scroll">
      <table className="loan-table">
        <thead>
          <tr>
            <th>Equipment</th>
            <th>Borrowed by</th>
            <th>Due back</th>
            <th>Qty</th>
            <th>Status</th>
            <th>
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((loan) => (
            <tr key={loan.id}>
              <td>
                <button className="loan-item-button" onClick={() => open(loan)}>
                  <span
                    className={`loan-thumbnail category-${loan.category.toLowerCase()}`}
                  >
                    <GearArt category={loan.category} />
                  </span>
                  <span>
                    <strong>{loan.equipmentName}</strong>
                    <small>{loan.equipmentModel || categoryNames[loan.category]}</small>
                  </span>
                </button>
              </td>
              <td>
                <span className="borrower-inline">
                  <span className="avatar">{initials(loan.borrowerName)}</span>
                  <span>{loan.borrowerName}</span>
                </span>
              </td>
              <td>
                <span className={loan.status === "OVERDUE" ? "overdue-text" : ""}>
                  {date(loan.dueDate)}
                </span>
              </td>
              <td>{loan.quantity}</td>
              <td>
                <LoanBadge loan={loan} />
              </td>
              <td>
                {loan.status !== "RETURNED" ? (
                  <button
                    className="return-link"
                    aria-label={`Return ${loan.equipmentName} from ${loan.borrowerName}`}
                    onClick={() => returnLoan(loan)}
                  >
                    <Check size={14} />
                    <span>Return</span>
                  </button>
                ) : (
                  <button
                    className="icon-button"
                    aria-label={`View returned ${loan.equipmentName}`}
                    onClick={() => open(loan)}
                  >
                    <ChevronRight size={17} />
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export function SearchBox({
  value,
  onChange,
  placeholder,
  label,
}: {
  value: string;
  onChange: (s: string) => void;
  placeholder: string;
  label: string;
}) {
  return (
    <label className="search-box">
      <Search size={18} />
      <input
        name="search"
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete="off"
      />
    </label>
  );
}
export function EquipmentView({
  page,
  filters,
  setFilters,
  add,
  open,
  edit,
  checkout,
  loading,
}: {
  page: Page<Equipment> | null;
  filters: Filters;
  setFilters: (f: Partial<Filters>) => void;
  add: () => void;
  open: (e: Equipment) => void;
  edit: (e: Equipment) => void;
  checkout: (e: Equipment) => void;
  loading: boolean;
}) {
  return (
    <section>
      <div className="view-heading">
        <div>
          <p className="view-context">The gear library</p>
          <h1>Find your next possibility.</h1>
          <p>A place for every camera, cable, and creative idea.</p>
        </div>
        <button className="button primary" onClick={add}>
          <Plus size={17} />
          Add equipment
        </button>
      </div>
      <div className="category-tabs" aria-label="Equipment categories">
        {["", ...categories].map((category) => (
          <button
            key={category}
            aria-pressed={filters.category === category}
            className={filters.category === category ? "selected" : ""}
            onClick={() => setFilters({ category, page: 1 })}
          >
            {category
              ? categoryNames[category as keyof typeof categoryNames]
              : "All gear"}
          </button>
        ))}
      </div>
      <div className="collection-toolbar">
        <SearchBox
          label="Search equipment"
          placeholder="Find a camera, laptop, or kit…"
          value={filters.search}
          onChange={(search) => setFilters({ search, page: 1 })}
        />
        <label className="filter-select">
          <SlidersHorizontal size={16} />
          <select
            aria-label="Equipment availability"
            value={filters.status}
            onChange={(e) => setFilters({ status: e.target.value, page: 1 })}
          >
            <option value="ALL">All availability</option>
            <option value="AVAILABLE">Available now</option>
            <option value="OUT">All checked out</option>
          </select>
        </label>
      </div>
      {loading && !page ? (
        <p className="loading-message" role="status">
          Finding your gear…
        </p>
      ) : page && page.items.length > 0 ? (
        <>
          <div className={`equipment-grid ${loading ? "refreshing" : ""}`}>
            {page.items.map((item) => (
              <GearCard
                key={item.id}
                item={item}
                open={open}
                edit={edit}
                checkout={checkout}
              />
            ))}
          </div>
          <Pagination page={page} setPage={(page) => setFilters({ page })} />
        </>
      ) : (
        <Empty
          title={
            filters.search || filters.category || filters.status !== "ALL"
              ? "No gear matches just yet."
              : "Give your gear a home."
          }
          description={
            filters.search || filters.category
              ? "Try another search or choose a different category."
              : "Add the first piece of equipment to start lending."
          }
        >
          <button className="button primary" onClick={add}>
            Add equipment
          </button>
        </Empty>
      )}
    </section>
  );
}
export function LoansView({
  page,
  filters,
  setFilters,
  add,
  open,
  returnLoan,
  loading,
}: {
  page: Page<Loan> | null;
  filters: Filters;
  setFilters: (f: Partial<Filters>) => void;
  add: () => void;
  open: (l: Loan) => void;
  returnLoan: (l: Loan) => void;
  loading: boolean;
}) {
  return (
    <section>
      <div className="view-heading">
        <div>
          <p className="view-context">Out in the world</p>
          <h1>A little lending. All together.</h1>
          <p>Who has what, and when it’s coming home.</p>
        </div>
        <button className="button primary" onClick={add}>
          <Plus size={17} />
          New loan
        </button>
      </div>
      <div className="category-tabs" aria-label="Loan status">
        {[
          ["ALL", "All loans"],
          ["ACTIVE", "On loan"],
          ["OVERDUE", "Overdue"],
          ["RETURNED", "Returned"],
        ].map(([status, label]) => (
          <button
            key={status}
            aria-pressed={filters.status === status}
            className={filters.status === status ? "selected" : ""}
            onClick={() => setFilters({ status, page: 1 })}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="collection-toolbar">
        <SearchBox
          label="Search loans"
          placeholder="Search equipment or a person…"
          value={filters.search}
          onChange={(search) => setFilters({ search, page: 1 })}
        />
        <span className="result-count">{page?.totalItems ?? 0} loans</span>
      </div>
      <div className="table-panel">
        {page?.items.length ? (
          <LoanTable items={page.items} open={open} returnLoan={returnLoan} />
        ) : (
          <Empty
            title={loading ? "Loading your loans…" : "Nothing on this list yet."}
            description="Create a loan when someone takes a piece of gear."
          />
        )}
        {page && <Pagination page={page} setPage={(page) => setFilters({ page })} />}
      </div>
    </section>
  );
}
export function PeopleView({
  page,
  filters,
  setFilters,
  add,
  edit,
  loading,
}: {
  page: Page<Borrower> | null;
  filters: Filters;
  setFilters: (f: Partial<Filters>) => void;
  add: () => void;
  edit: (p: Borrower) => void;
  loading: boolean;
}) {
  return (
    <section>
      <div className="view-heading">
        <div>
          <p className="view-context">The people behind the projects</p>
          <h1>Good things are a team effort.</h1>
          <p>Keep your borrowers and their kits in the picture.</p>
        </div>
        <button className="button primary" onClick={add}>
          <Plus size={17} />
          Add person
        </button>
      </div>
      <div className="collection-toolbar">
        <SearchBox
          label="Search people"
          placeholder="Find a person or team…"
          value={filters.search}
          onChange={(search) => setFilters({ search, page: 1 })}
        />
        <span className="result-count">{page?.totalItems ?? 0} people</span>
      </div>
      {page?.items.length ? (
        <>
          <div className="people-grid">
            {page.items.map((person) => (
              <article className="person-card" key={person.id}>
                <span className="avatar large">{initials(person.name)}</span>
                <div>
                  <h2>{person.name}</h2>
                  <p>{person.team || "Independent creative"}</p>
                  <span className="person-loans">
                    {person.activeLoans} active{" "}
                    {person.activeLoans === 1 ? "loan" : "loans"}
                  </span>
                </div>
                <button
                  className="icon-button"
                  aria-label={`Edit ${person.name}`}
                  onClick={() => edit(person)}
                >
                  <MoreHorizontal size={20} />
                </button>
              </article>
            ))}
          </div>
          <Pagination page={page} setPage={(page) => setFilters({ page })} />
        </>
      ) : (
        <Empty
          title={loading ? "Finding your people…" : "Every kit needs a creative."}
          description="Add someone before checking out equipment to them."
        >
          <button className="button primary" onClick={add}>
            Add person
          </button>
        </Empty>
      )}
    </section>
  );
}
