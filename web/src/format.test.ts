import { describe, expect, it } from "vitest";
import { maskBrlInput, parseBrlToCents } from "./format";

describe("máscara de real", () => {
  it("formata uma sequência de dígitos como centavos", () => {
    expect(maskBrlInput("1")).toBe("R$ 0,01");
    expect(maskBrlInput("123456")).toBe("R$ 1.234,56");
  });

  it("aceita colagem e mantém a conversão para centavos", () => {
    const masked = maskBrlInput("R$ 9.876,54");
    expect(masked).toBe("R$ 9.876,54");
    expect(parseBrlToCents(masked)).toBe(987_654);
  });

  it("permite limpar o campo", () => {
    expect(maskBrlInput("")).toBe("");
    expect(maskBrlInput("0")).toBe("");
  });
});
