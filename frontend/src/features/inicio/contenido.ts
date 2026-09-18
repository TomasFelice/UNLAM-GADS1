import type { NombreIcono } from "../../shared/components/Icono";

export const SEO = {
  titulo: "Gestión comercial para salones de eventos",
  descripcion:
    "Ztech CRM organiza empresas, contactos y oportunidades de tu salón de eventos corporativos en un solo embudo, sin planillas ni WhatsApp perdido.",
};

export const HERO = {
  kicker: "CRM para salones de eventos",
  titular: "Todas las oportunidades de tu salón de eventos, en un solo lugar",
  bajada:
    "Ztech CRM centraliza empresas, contactos y oportunidades para operadores de salones que alquilan espacios para eventos corporativos de hasta 50 personas, y controla la disponibilidad de cada fecha antes de confirmar una reserva.",
  ctaPrimario: "Conocé el CRM",
  ctaSecundario: "Cómo funciona",
};

export const PROBLEMA = {
  titulo: "Cuando la agenda del salón vive en planillas",
  bajada:
    "Sin un sistema único, la información comercial de un salón de eventos queda repartida entre mails, WhatsApp y planillas que nadie actualiza a tiempo.",
  items: [
    {
      titulo: "Historial disperso",
      texto:
        "Cada consulta queda en un mail distinto o en la memoria del vendedor que la atendió, y esa información se pierde cuando esa persona no está.",
    },
    {
      titulo: "Negociaciones sin seguimiento",
      texto:
        "Nadie sabe con certeza cuántas oportunidades están abiertas, en qué etapa quedaron ni por qué se cayeron las que no se cerraron.",
    },
    {
      titulo: "Riesgo de sobreventa",
      texto:
        "Sin un control de disponibilidad, dos vendedores pueden confirmar el mismo salón para el mismo horario sin enterarse hasta el día del evento.",
    },
    {
      titulo: "Dependencia de una persona",
      texto:
        "Cuando toda la información comercial vive en la cabeza de un vendedor, el negocio se frena apenas esa persona falta o deja el puesto.",
    },
  ],
};

export const CAPACIDADES: {
  titulo: string;
  bajada: string;
  items: { icono: NombreIcono; titulo: string; texto: string }[];
} = {
  titulo: "Todo el proceso comercial, en un mismo lugar",
  bajada:
    "Ztech CRM cubre el recorrido completo de una oportunidad: desde el primer contacto con la empresa hasta la confirmación de la reserva del salón.",
  items: [
    {
      icono: "empresas",
      titulo: "Empresas y contactos",
      texto:
        "Registrá cada empresa cliente con sus contactos asociados, para saber quién pidió qué salón, cuándo y a través de qué persona de esa organización.",
    },
    {
      icono: "oportunidades",
      titulo: "Oportunidades comerciales",
      texto:
        "Cada consulta se convierte en una oportunidad con un salón, un responsable comercial y una fecha propuesta, sin depender de memoria ni de planillas sueltas.",
    },
    {
      icono: "embudo",
      titulo: "Embudo por etapas",
      texto:
        "Un tablero muestra en qué etapa está cada oportunidad, desde el primer contacto hasta el cierre, para ver de un vistazo dónde está trabado el negocio.",
    },
    {
      icono: "salones",
      titulo: "Salones y disponibilidad",
      texto:
        "Cada salón tiene su capacidad y su agenda propia, y el sistema controla que no se confirmen dos reservas ganadas para la misma fecha y horario.",
    },
    {
      icono: "capas",
      titulo: "Actividades e historial",
      texto:
        "Cada llamado, mail o reunión queda registrado como actividad de la oportunidad, con fecha y responsable, así el historial comercial no depende de un solo vendedor.",
    },
    {
      icono: "escudo",
      titulo: "Roles y permisos",
      texto:
        "Cada usuario entra al sistema con un rol definido (administrador, responsable comercial o vendedor), y sólo ve y modifica lo que corresponde a su función.",
    },
  ],
};

export const COMO_FUNCIONA = {
  titulo: "De la primera consulta a la reserva confirmada",
  pasos: [
    {
      titulo: "Registrá la empresa",
      texto:
        "Cargá la empresa que consulta y la persona de contacto con la que vas a hablar, con sus datos en un mismo lugar desde el inicio.",
    },
    {
      titulo: "Creá la oportunidad",
      texto:
        "Abrí la oportunidad con el salón solicitado, la fecha propuesta y el responsable comercial que va a llevar adelante la negociación con esa empresa.",
    },
    {
      titulo: "Movela por el embudo",
      texto:
        "A medida que avanza la negociación, cambiá la oportunidad de etapa y registrá cada llamado o reunión como actividad, con fecha y responsable.",
    },
    {
      titulo: "Cerrala ganada o perdida",
      texto:
        "Al cierre, marcá la oportunidad como ganada (y el sistema valida la disponibilidad del salón) o como perdida, dejando registrado el motivo.",
    },
  ],
};

export const CIERRE = {
  titulo: "Un solo sistema para el control comercial de tu salón",
  bajada:
    "Conocé cómo Ztech CRM organiza empresas, oportunidades y disponibilidad de salones para eventos corporativos, y evitá que una reserva se pierda por falta de seguimiento.",
  cta: "Ver el producto",
};
