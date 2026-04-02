import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { importApi } from "../api/importApi";
import { LanguageProvider } from "../i18n/LanguageProvider";
import ImportCsvPage from "./ImportCsvPage";

vi.mock("../api/importApi", () => ({
  importApi: {
    getConsentStatus: vi.fn(),
    grantConsent: vi.fn(),
    revokeConsent: vi.fn(),
    getIntegrationStatus: vi.fn(),
    startOAuth: vi.fn(),
    disconnect: vi.fn(),
    syncMailbox: vi.fn(),
    start: vi.fn(),
    history: vi.fn(),
    getById: vi.fn()
  }
}));

const mockedImportApi = vi.mocked(importApi);

describe("ImportCsvPage consent gating", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
    window.localStorage.setItem("subtrack.language", "EN");

    mockedImportApi.getConsentStatus.mockResolvedValue({
      provider: "GMAIL",
      status: "NOT_GRANTED",
      scope: null,
      grantedAt: null,
      revokedAt: null,
      integrationStatus: "NOT_CONNECTED"
    });
    mockedImportApi.getIntegrationStatus.mockResolvedValue({
      id: null,
      provider: "GMAIL",
      status: "NOT_CONNECTED",
      externalAccountEmail: null,
      connectedAt: null,
      updatedAt: null,
      lastSyncAt: null,
      lastErrorCode: null,
      lastErrorMessage: null
    });
    mockedImportApi.history.mockResolvedValue([]);
    mockedImportApi.grantConsent.mockResolvedValue({
      provider: "GMAIL",
      status: "GRANTED",
      scope: "https://www.googleapis.com/auth/gmail.readonly",
      grantedAt: "2026-03-20T09:00:00Z",
      revokedAt: null,
      integrationStatus: "NOT_CONNECTED"
    });
    mockedImportApi.startOAuth.mockResolvedValue({
      provider: "GMAIL",
      authorizationUrl: "https://accounts.google.com/o/oauth2/auth"
    });
  });

  const renderPage = () =>
    render(
      <MemoryRouter>
        <LanguageProvider>
          <ImportCsvPage />
        </LanguageProvider>
      </MemoryRouter>
    );

  it("opens consent dialog before Gmail connection starts", async () => {
    const user = userEvent.setup();
    mockedImportApi.startOAuth.mockRejectedValueOnce(new Error("OAuth blocked"));

    renderPage();

    await user.click(await screen.findByRole("button", { name: "Connect Gmail" }));

    const dialog = await screen.findByRole("dialog", { name: "Import consent" });
    expect(mockedImportApi.grantConsent).not.toHaveBeenCalled();
    expect(mockedImportApi.startOAuth).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole("button", { name: "Connect Gmail" }));

    await waitFor(() => {
      expect(mockedImportApi.grantConsent).toHaveBeenCalledTimes(1);
      expect(mockedImportApi.startOAuth).toHaveBeenCalledTimes(1);
    });

    expect(await within(dialog).findByText("OAuth blocked")).toBeInTheDocument();
  });

  it("keeps grant consent behind the same dialog flow", async () => {
    const user = userEvent.setup();

    renderPage();

    await user.click(await screen.findByRole("button", { name: "Grant consent" }));

    const dialog = await screen.findByRole("dialog", { name: "Import consent" });
    expect(mockedImportApi.grantConsent).not.toHaveBeenCalled();
    expect(mockedImportApi.startOAuth).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole("button", { name: "Grant consent" }));

    await waitFor(() => {
      expect(mockedImportApi.grantConsent).toHaveBeenCalledTimes(1);
    });

    expect(mockedImportApi.startOAuth).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog", { name: "Import consent" })).not.toBeInTheDocument();
  });
});
