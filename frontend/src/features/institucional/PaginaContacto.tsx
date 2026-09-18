import { useState, type FormEvent } from "react";
import { Seo } from "../../shared/seo/Seo";
import { migas, preguntasFrecuentes } from "../../shared/seo/datosEstructurados";
import { Icono } from "../../shared/components/Icono";
import { Boton } from "../../shared/components/Boton";
import {
  AreaTexto,
  Campo,
  Entrada,
  Selector,
} from "../../shared/components/Campos";
import { SITIO } from "../../shared/seo/sitio";
import {
  CONTACTO_HERO,
  MOTIVOS_CONSULTA,
  PREGUNTAS,
  SEO_CONTACTO,
} from "./contenido";
import css from "./Institucional.module.css";

export function PaginaContacto() {
  const [enviado, setEnviado] = useState(false);

  /**
   * La maqueta no envía nada: no hay servicio de correo ni endpoint. El
   * formulario muestra el estado de confirmación para poder validar el
   * diseño del flujo completo (ver DF-05).
   */
  const alEnviar = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setEnviado(true);
  };

  return (
    <>
      <Seo
        titulo={SEO_CONTACTO.titulo}
        descripcion={SEO_CONTACTO.descripcion}
        canonica="/contacto"
        datosEstructurados={[
          migas([
            { nombre: "Inicio", ruta: "/" },
            { nombre: "Contacto", ruta: "/contacto" },
          ]),
          preguntasFrecuentes(PREGUNTAS),
        ]}
      />

      <header className={css.portada}>
        <div className={css.contenedor}>
          <p className={css.migaSimple}>Contacto</p>
          <h1>{CONTACTO_HERO.titular}</h1>
          <p className={css.portadaBajada}>{CONTACTO_HERO.bajada}</p>
        </div>
      </header>

      <div className={css.contenedor}>
        <section className={css.seccion}>
          <div className={css.contactoGrilla}>
            {/* --- Formulario ------------------------------------------ */}
            <div className={css.formularioCaja}>
              {enviado ? (
                <div className={css.confirmacion} role="status">
                  <span className={css.confirmacionIcono}>
                    <Icono nombre="check" tamano={22} />
                  </span>
                  <h2>Recibimos tu consulta</h2>
                  <p>
                    Gracias por escribirnos. Te vamos a responder al correo que
                    dejaste.
                  </p>
                  <p className={css.confirmacionNota}>
                    Esta página es una maqueta: el formulario todavía no envía
                    información a ningún servidor.
                  </p>
                  <Boton
                    variante="secundario"
                    onClick={() => setEnviado(false)}
                    icono="flechaIzquierda"
                  >
                    Volver al formulario
                  </Boton>
                </div>
              ) : (
                <form onSubmit={alEnviar} noValidate>
                  <h2 className={css.formularioTitulo}>Escribinos</h2>
                  <p className={css.formularioBajada}>
                    Los campos marcados con asterisco son obligatorios.
                  </p>

                  <div className={css.formularioGrilla}>
                    <Campo etiqueta="Nombre y apellido" requerido>
                      {(id) => (
                        <Entrada
                          id={id}
                          name="nombre"
                          autoComplete="name"
                          placeholder="Carolina Suárez"
                          required
                        />
                      )}
                    </Campo>

                    <Campo etiqueta="Correo electrónico" requerido>
                      {(id) => (
                        <Entrada
                          id={id}
                          name="email"
                          type="email"
                          autoComplete="email"
                          placeholder="carolina@empresa.com.ar"
                          required
                        />
                      )}
                    </Campo>

                    <Campo etiqueta="Empresa o salón">
                      {(id) => (
                        <Entrada
                          id={id}
                          name="empresa"
                          autoComplete="organization"
                          placeholder="Salón Panorama"
                        />
                      )}
                    </Campo>

                    <Campo etiqueta="Teléfono">
                      {(id) => (
                        <Entrada
                          id={id}
                          name="telefono"
                          type="tel"
                          autoComplete="tel"
                          placeholder="+54 9 11 5555-5555"
                        />
                      )}
                    </Campo>

                    <Campo etiqueta="Motivo de la consulta" requerido ancho>
                      {(id) => (
                        <Selector id={id} name="motivo" defaultValue="" required>
                          <option value="" disabled>
                            Elegí un motivo
                          </option>
                          {MOTIVOS_CONSULTA.map((m) => (
                            <option key={m} value={m}>
                              {m}
                            </option>
                          ))}
                        </Selector>
                      )}
                    </Campo>

                    <Campo
                      etiqueta="Tu consulta"
                      requerido
                      ancho
                      ayuda="Contanos qué necesitás. Cuanto más concreto, mejor te podemos responder."
                    >
                      {(id, ayuda) => (
                        <AreaTexto
                          id={id}
                          name="mensaje"
                          aria-describedby={ayuda}
                          rows={5}
                          placeholder="Operamos dos salones en Vicente López y queremos ordenar el seguimiento de las consultas."
                          required
                        />
                      )}
                    </Campo>
                  </div>

                  <label className={css.consentimiento}>
                    <input type="checkbox" name="consentimiento" required />
                    <span>
                      Acepto que mis datos se traten según la{" "}
                      <a href="/privacidad">Política de Privacidad</a>.
                    </span>
                  </label>

                  <div className={css.formularioPie}>
                    <Boton type="submit" tamano="lg" iconoFinal="flechaDerecha">
                      Enviar consulta
                    </Boton>
                    <p className={css.avisoMaqueta}>
                      Formulario académico: el envío no sale del navegador.
                    </p>
                  </div>
                </form>
              )}
            </div>

            {/* --- Datos de contacto ------------------------------------ */}
            <aside className={css.datosContacto}>
              <h2>Otros canales</h2>

              <ul className={css.listaDatos}>
                <li>
                  <span className={css.datoIcono}>
                    <Icono nombre="correo" tamano={17} />
                  </span>
                  <div>
                    <p className={css.datoEtiqueta}>Correo</p>
                    <a href={`mailto:${SITIO.email}`}>{SITIO.email}</a>
                  </div>
                </li>
                <li>
                  <span className={css.datoIcono}>
                    <Icono nombre="ubicacion" tamano={17} />
                  </span>
                  <div>
                    <p className={css.datoEtiqueta}>Dónde estamos</p>
                    <p>{SITIO.localidad}</p>
                  </div>
                </li>
                <li>
                  <span className={css.datoIcono}>
                    <Icono nombre="reloj" tamano={17} />
                  </span>
                  <div>
                    <p className={css.datoEtiqueta}>Tiempo de respuesta</p>
                    <p>Respondemos dentro de los días hábiles siguientes.</p>
                  </div>
                </li>
              </ul>

              <div className={css.notaLateral}>
                <p>
                  Ztech CRM es un trabajo práctico de {SITIO.materia}, en la{" "}
                  {SITIO.institucion}. No comercializamos el producto ni
                  ofrecemos soporte contractual.
                </p>
              </div>
            </aside>
          </div>
        </section>

        {/* --- Preguntas frecuentes ------------------------------------- */}
        <section
          id="preguntas"
          className={css.seccion}
          aria-labelledby="titulo-preguntas"
        >
          <div className={css.dosColumnas}>
            <h2 id="titulo-preguntas" className={css.tituloLateral}>
              Preguntas frecuentes
            </h2>

            <div className={css.preguntas}>
              {PREGUNTAS.map((p) => (
                <details key={p.pregunta} className={css.pregunta}>
                  <summary>
                    {p.pregunta}
                    <Icono nombre="chevronAbajo" tamano={16} />
                  </summary>
                  <p>{p.respuesta}</p>
                </details>
              ))}
            </div>
          </div>
        </section>
      </div>
    </>
  );
}
