import { describe, expect, it } from "vitest";
import { inferAssetType } from "./Investments";

describe("detecção contextual de investimentos", () => {
  it("reconhece tickers B3 como ação", () => {
    expect(inferAssetType("fiqe3")).toBe("stock");
    expect(inferAssetType("PETR4")).toBe("stock");
  });

  it("reconhece criptomoedas pelo nome e símbolo", () => {
    expect(inferAssetType("bitcoin")).toBe("crypto");
    expect(inferAssetType("Ethereum")).toBe("crypto");
    expect(inferAssetType("BTC")).toBe("crypto");
  });

  it("reconhece renda fixa pelo contexto", () => {
    expect(inferAssetType("CDB Banco X")).toBe("cdb");
    expect(inferAssetType("Tesouro Selic 2029")).toBe("treasury");
  });
});
