export type ErrorDeCampo = {
  field: string;
  message: string;
};

export type DetalleDeProblema = {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  code?: string;
  requestId?: string;
  errors: ErrorDeCampo[];
};

export class ErrorDeApi extends Error {
  readonly problema: DetalleDeProblema;

  constructor(problema: DetalleDeProblema) {
    super(problema.detail);
    this.name = "ErrorDeApi";
    this.problema = problema;
  }
}

const esErrorDeCampo = (valor: unknown): valor is ErrorDeCampo => {
  if (!valor || typeof valor !== "object") return false;
  const candidato = valor as Record<string, unknown>;
  return typeof candidato.field === "string" && typeof candidato.message === "string";
};

export async function interpretarProblema(respuesta: Response): Promise<DetalleDeProblema> {
  let cuerpo: Record<string, unknown> = {};
  try {
    cuerpo = (await respuesta.json()) as Record<string, unknown>;
  } catch {
    // Algunas fallas de infraestructura no conservan el ProblemDetail del backend.
  }

  return {
    type: typeof cuerpo.type === "string" ? cuerpo.type : undefined,
    title: typeof cuerpo.title === "string" ? cuerpo.title : undefined,
    status: respuesta.status,
    detail:
      typeof cuerpo.detail === "string"
        ? cuerpo.detail
        : "No se pudo completar la operación. Intentá nuevamente.",
    code: typeof cuerpo.code === "string" ? cuerpo.code : undefined,
    requestId: typeof cuerpo.requestId === "string" ? cuerpo.requestId : undefined,
    errors: Array.isArray(cuerpo.errors) ? cuerpo.errors.filter(esErrorDeCampo) : [],
  };
}
