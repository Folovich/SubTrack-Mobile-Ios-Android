export const formatCurrency = (value: number, currency?: string | null) => {
  const amount = Number.isFinite(value) ? value : 0;
  const normalizedCurrency = currency?.trim().toUpperCase();

  if (!normalizedCurrency) {
    return new Intl.NumberFormat("en-US", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  }

  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: normalizedCurrency
    }).format(amount);
  } catch {
    return `${new Intl.NumberFormat("en-US", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount)} ${normalizedCurrency}`;
  }
};
