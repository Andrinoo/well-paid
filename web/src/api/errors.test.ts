import { describe, expect, it } from "vitest";
import { ApiError, apiErrorDetails } from "./errors";

describe("apiErrorDetails", () => {
  it("preserva o contrato estruturado da API", () => {
    expect(
      apiErrorDetails(
        {
          code: "expense_split_sum_mismatch",
          message: "A divisão não fecha com o total.",
          fields: { peer_share_cents: "invalid" },
        },
        "Falha",
      ),
    ).toEqual({
      code: "expense_split_sum_mismatch",
      message: "A divisão não fecha com o total.",
      fields: { peer_share_cents: "invalid" },
    });
  });

  it("continua compatível com detail textual do FastAPI", () => {
    expect(apiErrorDetails({ detail: "not_allowed" }, "Falha")).toEqual({
      code: "not_allowed",
      message: "not_allowed",
      fields: {},
    });
  });

  it("usa a primeira mensagem de validação do Pydantic", () => {
    expect(apiErrorDetails({ detail: [{ msg: "Campo obrigatório" }] }, "Falha").message).toBe(
      "Campo obrigatório",
    );
  });
});
describe("ApiError", () => {
  it("expõe status, código e campos para as telas", () => {
    const error = new ApiError("Inválido", 422, {
      code: "validation_error",
      fields: { title: "required" },
    });
    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe("ApiError");
    expect(error.status).toBe(422);
    expect(error.code).toBe("validation_error");
    expect(error.fields.title).toBe("required");
  });
});
