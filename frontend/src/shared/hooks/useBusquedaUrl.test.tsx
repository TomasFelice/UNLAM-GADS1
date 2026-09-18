import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useBusquedaUrl } from "./useBusquedaUrl";

function BancoDePrueba() {
  const url = useBusquedaUrl();
  const ubicacion = useLocation();
  return <>
    <input aria-label="Buscar" value={url.busqueda} onChange={(e) => url.setBusqueda(e.target.value)} />
    <button onClick={() => url.cambiar("status", "ACTIVO")}>Filtrar</button>
    <button onClick={() => url.cambiarPagina(2)}>Paginar</button>
    <output>{ubicacion.search}</output>
  </>;
}

describe("filtros mediante URL", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("aplica debounce a la búsqueda y reinicia la página", () => {
    render(<MemoryRouter initialEntries={["/listado?page=3&status=POTENCIAL"]}><BancoDePrueba /></MemoryRouter>);

    fireEvent.change(screen.getByLabelText("Buscar"), { target: { value: "Andina" } });
    expect(screen.getByText(/page=3/)).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(350));

    expect(screen.getByText(/q=Andina/)).toBeInTheDocument();
    expect(screen.getByText(/status=POTENCIAL/)).toBeInTheDocument();
    expect(screen.queryByText(/page=3/)).not.toBeInTheDocument();
  });

  it("conserva filtros al paginar y quita page al cambiar un criterio", () => {
    render(<MemoryRouter initialEntries={["/listado?q=evento"]}><BancoDePrueba /></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Paginar" }));
    expect(screen.getByText(/page=2/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Filtrar" }));
    expect(screen.getByText(/status=ACTIVO/)).toBeInTheDocument();
    expect(screen.queryByText(/page=2/)).not.toBeInTheDocument();
  });
});
