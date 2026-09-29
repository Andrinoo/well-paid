import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { GoalThumb } from "./GoalThumb";

describe("GoalThumb", () => {
  it("tenta proxy, URL original e então mostra o ícone padrão", () => {
    const original = "https://images.store.example/product.jpg";
    render(<GoalThumb url={original} alt="Produto" />);

    const proxyImage = screen.getByRole("img", { name: "Produto" });
    expect(proxyImage).toHaveAttribute("src", expect.stringContaining("/media/thumbnail"));

    fireEvent.error(proxyImage);
    const directImage = screen.getByRole("img", { name: "Produto" });
    expect(directImage).toHaveAttribute("src", original);

    fireEvent.error(directImage);
    expect(screen.queryByRole("img", { name: "Produto" })).not.toBeInTheDocument();
  });
});
