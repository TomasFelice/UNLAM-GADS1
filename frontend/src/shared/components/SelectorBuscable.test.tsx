import { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Campo } from "./Campos";
import { SelectorBuscable } from "./SelectorBuscable";

function Ejemplo({ multiple = false, verMas = vi.fn() }) {
  const [busqueda, alBuscar] = useState(""); const [abierto, alAbrir] = useState(false);
  const [valores, cambiar] = useState(["1"]);
  const opciones = [{ valor: "1", texto: "Catering" }, { valor: "2", texto: "Audio" }];
  return <><Campo etiqueta="Servicios" ayuda="Servicios del evento">{(id, descripcion) => <SelectorBuscable
    {...{ id, descripcion, busqueda, alBuscar, abierto, alAbrir, multiple, verMas }} nombre="Servicios" marcador="Sin servicios"
    opciones={opciones.filter(o => o.texto.toLowerCase().includes(busqueda.toLowerCase()))}
    seleccionados={opciones.filter(o => valores.includes(o.valor))} cargando={false} cargandoMas={false} hayMas
    reintentar={() => {}} alCambiar={valor => cambiar(multiple ? valores.includes(valor) ? valores.filter(v => v !== valor) : [...valores, valor] : valor ? [valor] : [])} />}</Campo>
    <button type="button">Siguiente campo</button></>;
}

describe("selector integrado", () => {
  it("muestra un campo, permite buscar/elegir por teclado y cancela la búsqueda conservando el valor", async () => {
    render(<Ejemplo />); const persona = userEvent.setup();
    const control = screen.getByRole("combobox", { name: "Servicios" });
    expect(control).toHaveValue("Catering"); expect(control).toHaveAccessibleDescription("Servicios del evento");
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Ver más resultados" })).not.toBeInTheDocument();
    await persona.click(control); await persona.type(control, "audio");
    await persona.keyboard("{ArrowDown}{ArrowDown}{Enter}");
    expect(control).toHaveValue("Audio"); expect(control).toHaveFocus(); expect(control).toHaveAttribute("aria-expanded", "false");
    await persona.click(control); await persona.type(control, "inexistente");
    expect(screen.getByRole("status")).toHaveTextContent("Sin resultados");
    await persona.keyboard("{Escape}"); expect(control).toHaveValue("Audio");
    await persona.click(control); await persona.keyboard("{ArrowDown}{Enter}"); expect(control).toHaveValue("");
    await persona.click(control); await persona.keyboard("{Tab}");
    expect(screen.getByRole("button", { name: "Siguiente campo" })).toHaveFocus(); expect(control).toHaveAttribute("aria-expanded", "false");
  });

  it("mantiene varias selecciones al buscar, permite quitarlas y activa ver más con Enter", async () => {
    const verMas = vi.fn(); render(<Ejemplo multiple verMas={verMas} />); const persona = userEvent.setup();
    const control = screen.getByRole("combobox", { name: "Servicios" });
    await persona.click(control); await persona.type(control, "Audio");
    await persona.click(screen.getByRole("option", { name: "Audio" }));
    expect(control).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("button", { name: "Quitar Catering" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Quitar Audio" })).toBeInTheDocument();
    await persona.keyboard("{ArrowDown}{Enter}");
    expect(verMas).toHaveBeenCalledOnce();
    await persona.keyboard("{Escape}"); expect(control).toHaveAttribute("aria-expanded", "false");
    await persona.click(screen.getByRole("button", { name: "Quitar Catering" }));
    expect(screen.queryByRole("button", { name: "Quitar Catering" })).not.toBeInTheDocument();
    expect(control).toHaveValue("1 servicio seleccionado");
  });
});
