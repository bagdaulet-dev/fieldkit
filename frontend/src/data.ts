import { useCallback, useEffect, useState } from "react";
import { ApiClient } from "./api";
import type {
  Borrower,
  Dashboard,
  Equipment,
  Filters,
  Loan,
  Page,
  Session,
  View,
} from "./types";
export function useFieldkit() {
  const [client] = useState(() => new ApiClient());
  const [session, setSession] = useState<Session | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  const refresh = useCallback(() => setVersion((n) => n + 1), []);
  const connect = useCallback(async () => {
    setError(null);
    try {
      const data = await client.request<Session>("/session");
      client.csrf = data.csrfToken;
      setSession(data);
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, [client]);
  useEffect(() => {
    void connect();
  }, [connect]);
  return { client, session, setSession, connect, error, loading, refresh, version };
}
export function useWorkspace(
  client: ApiClient,
  session: Session | null,
  view: View,
  filters: Filters,
  version: number,
) {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [equipment, setEquipment] = useState<Page<Equipment> | null>(null);
  const [loans, setLoans] = useState<Page<Loan> | null>(null);
  const [people, setPeople] = useState<Page<Borrower> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [search, setSearch] = useState(filters.search);
  useEffect(() => {
    const timer = setTimeout(() => setSearch(filters.search), 230);
    return () => clearTimeout(timer);
  }, [filters.search]);
  useEffect(() => {
    if (!session) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    const query = new URLSearchParams({
      page: String(filters.page),
      pageSize: "12",
      search,
    });
    if (view === "equipment") {
      if (filters.category) query.set("category", filters.category);
      if (filters.status !== "ALL") query.set("status", filters.status);
    }
    if (view === "loans" && filters.status !== "ALL") query.set("status", filters.status);
    const list =
      view === "equipment"
        ? client
            .request<Page<Equipment>>(`/equipment?${query}`, {
              signal: controller.signal,
            })
            .then(setEquipment)
        : view === "loans"
          ? client
              .request<Page<Loan>>(`/loans?${query}`, { signal: controller.signal })
              .then(setLoans)
          : view === "people"
            ? client
                .request<Page<Borrower>>(`/borrowers?${query}`, {
                  signal: controller.signal,
                })
                .then(setPeople)
            : Promise.resolve();
    Promise.all([
      client
        .request<Dashboard>("/dashboard", { signal: controller.signal })
        .then(setDashboard),
      list,
    ])
      .catch((e) => {
        if (!controller.signal.aborted) setError(e);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [
    client,
    session,
    view,
    filters.category,
    filters.status,
    filters.page,
    search,
    version,
  ]);
  return { dashboard, equipment, loans, people, loading, error };
}
