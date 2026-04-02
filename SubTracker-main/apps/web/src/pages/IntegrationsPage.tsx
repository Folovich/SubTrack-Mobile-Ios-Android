import { useEffect, useState } from "react";
import { integrationApi } from "../api/integrationApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { IntegrationConnection } from "../types/integration";

const formatDateTime = (value: string | null) => {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
};

const IntegrationsPage = () => {
  const { t } = useLanguage();
  const [items, setItems] = useState<IntegrationConnection[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setError("");
      setIsLoading(true);

      try {
        const response = await integrationApi.list();
        setItems(response);
      } catch (loadError) {
        setItems([]);
        setError(loadError instanceof Error ? loadError.message : t("common_error"));
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, []);

  return (
    <div className="page">
      <h1 className="page__title">{t("integrations_title")}</h1>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("integrations_connections")}</h2>
        {items.length ? (
          <div className="stack-list">
            {items.map((item) => (
              <div key={`${item.provider}-${item.id ?? "none"}`} className="card-row">
                <strong>{item.provider}</strong>
                <span>{t("integrations_status")}: {item.status}</span>
                <span>{t("integrations_account")}: {item.externalAccountEmail ?? t("common_none")}</span>
                <span>{t("integrations_connected")}: {formatDateTime(item.connectedAt) || t("import_not_available")}</span>
                <span>{t("integrations_updated")}: {formatDateTime(item.updatedAt) || t("import_not_available")}</span>
                <span>{t("integrations_last_sync")}: {formatDateTime(item.lastSyncAt) || t("import_not_available")}</span>
                {item.lastErrorMessage ? (
                  <span>
                    {t("integrations_last_error")}: {item.lastErrorCode ? `${item.lastErrorCode} · ` : ""}
                    {item.lastErrorMessage}
                  </span>
                ) : null}
              </div>
            ))}
          </div>
        ) : (
          <p>{isLoading ? t("common_loading") : t("integrations_empty")}</p>
        )}
      </section>
    </div>
  );
};

export default IntegrationsPage;
