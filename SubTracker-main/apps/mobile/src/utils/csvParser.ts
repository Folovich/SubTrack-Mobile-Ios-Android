export const parseCsv = (csvText: string) => {
  return csvText.split("\n").map((line) => line.split(","));
};
