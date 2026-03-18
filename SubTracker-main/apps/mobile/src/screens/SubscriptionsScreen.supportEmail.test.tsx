import React from "react";
import { fireEvent, render, waitFor } from "@testing-library/react-native";
import { Linking } from "react-native";
import * as Clipboard from "expo-clipboard";
import { categoryApi } from "../api/categoryApi";
import { subscriptionApi } from "../api/subscriptionApi";
import SubscriptionsScreen from "./SubscriptionsScreen";

jest.mock("../context/SettingsContext", () => ({
  useI18n: () => ({
    tr: (key: string) => key,
    colors: {
      bg: "#ffffff",
      bgElevated: "#f8f8f8",
      bgSoft: "#f0f0f0",
      card: "#ffffff",
      border: "#d0d0d0",
      text: "#111111",
      textMuted: "#666666",
      accent: "#0066ff",
      accentStrong: "#3388ff",
      accentText: "#ffffff",
      danger: "#cc0000",
      shadow: "rgba(0,0,0,0.2)",
      overlay: "#111111",
      sidebar: "#111111",
      sidebarText: "#ffffff",
      secondaryButton: "#efefef",
      focus: "#88aaff"
    }
  })
}));

jest.mock("../api/categoryApi", () => ({
  categoryApi: {
    getAll: jest.fn()
  }
}));

jest.mock("../api/subscriptionApi", () => ({
  subscriptionApi: {
    getAll: jest.fn(),
    getById: jest.fn(),
    create: jest.fn(),
    update: jest.fn(),
    remove: jest.fn(),
    getUpcoming: jest.fn(),
    getSupportEmailDraft: jest.fn(),
    trackSupportEmailEvent: jest.fn()
  }
}));

const mockedCategoryApi = categoryApi as jest.Mocked<typeof categoryApi>;
const mockedSubscriptionApi = subscriptionApi as jest.Mocked<typeof subscriptionApi>;

describe("SubscriptionsScreen support email flow", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockedCategoryApi.getAll.mockResolvedValue([{ id: 1, name: "Entertainment" }]);
    mockedSubscriptionApi.getAll.mockResolvedValue([
      {
        id: 7,
        serviceName: "Netflix",
        category: "Entertainment",
        amount: 9.99,
        currency: "USD",
        billingPeriod: "MONTHLY",
        nextBillingDate: "2026-03-28",
        status: "ACTIVE"
      }
    ]);
    mockedSubscriptionApi.getSupportEmailDraft.mockResolvedValue({
      subscriptionId: 7,
      action: "CANCEL",
      provider: "GMAIL",
      draft: {
        to: "support@netflix.com",
        subject: "Cancel Netflix",
        body: "Hello support",
        mailtoUrl: "mailto:support@netflix.com?subject=Cancel&body=Hello",
        plainTextForCopy: "To: support@netflix.com\nSubject: Cancel Netflix\n\nHello support"
      }
    });
    mockedSubscriptionApi.trackSupportEmailEvent.mockResolvedValue(undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("renders quick cancel and quick pause buttons", async () => {
    const { findByText, queryByText } = render(<SubscriptionsScreen />);
    await waitFor(() => expect(queryByText("loading")).toBeNull());

    expect(await findByText("quickCancel")).toBeTruthy();
    expect(await findByText("quickPause")).toBeTruthy();
  });

  it("loads support draft and handles copy + mailto actions", async () => {
    const canOpenSpy = jest.spyOn(Linking, "canOpenURL").mockResolvedValue(true);
    const openSpy = jest.spyOn(Linking, "openURL").mockResolvedValue(undefined);
    const copySpy = jest.spyOn(Clipboard, "setStringAsync").mockResolvedValue(true);

    const { findByText, queryByText } = render(<SubscriptionsScreen />);
    await waitFor(() => expect(queryByText("loading")).toBeNull());

    fireEvent.press((await findByText("quickCancel")));
    expect(await findByText("supportDraftTitle")).toBeTruthy();

    fireEvent.press(await findByText("copyEmailText"));
    await waitFor(() =>
      expect(copySpy).toHaveBeenCalledWith("To: support@netflix.com\nSubject: Cancel Netflix\n\nHello support")
    );

    fireEvent.press(await findByText("openMailClient"));
    await waitFor(() => {
      expect(canOpenSpy).toHaveBeenCalledWith("mailto:support@netflix.com?subject=Cancel&body=Hello");
      expect(openSpy).toHaveBeenCalledWith("mailto:support@netflix.com?subject=Cancel&body=Hello");
    });

    expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "DRAFT_OPENED");
    expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "TEXT_COPIED");
    expect(mockedSubscriptionApi.trackSupportEmailEvent).toHaveBeenCalledWith(7, "CANCEL", "MAILTO_OPENED");
  });

  it("shows error when support draft API fails", async () => {
    mockedSubscriptionApi.getSupportEmailDraft.mockRejectedValueOnce("broken");
    const { findByText, queryByText } = render(<SubscriptionsScreen />);
    await waitFor(() => expect(queryByText("loading")).toBeNull());

    fireEvent.press(await findByText("quickPause"));

    await waitFor(() => {
      expect(mockedSubscriptionApi.getSupportEmailDraft).toHaveBeenCalledWith(7, "PAUSE");
    });
    expect(await findByText("supportDraftLoadError")).toBeTruthy();
  });
});
