export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export class ApiClient {
  csrf = "";
  async request<T>(
    path: string,
    options: { method?: string; body?: unknown; signal?: AbortSignal } = {},
  ): Promise<T> {
    const method = options.method || "GET";
    const headers: Record<string, string> = { Accept: "application/json" };
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    if (!["GET", "HEAD"].includes(method)) headers["X-CSRF-TOKEN"] = this.csrf;
    let response: Response;
    try {
      response = await fetch(`/api${path}`, {
        method,
        headers,
        credentials: "same-origin",
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: options.signal
          ? AbortSignal.any([options.signal, AbortSignal.timeout(15000)])
          : AbortSignal.timeout(15000),
      });
    } catch {
      if (options.signal?.aborted) throw new ApiError("CANCELED", "Request canceled.", 0);
      throw new ApiError(
        "CONNECTION_ERROR",
        "Cannot reach Fieldkit. Check the server and refresh before retrying a change.",
        0,
      );
    }
    if (response.status === 204) return undefined as T;
    const data = (await response.json().catch(() => null)) as {
      error?: { code?: string; message?: string };
    } | null;
    if (!response.ok)
      throw new ApiError(
        data?.error?.code || "REQUEST_FAILED",
        data?.error?.message ||
          "This request could not be completed. Refresh and try again.",
        response.status,
      );
    return data as T;
  }
}
