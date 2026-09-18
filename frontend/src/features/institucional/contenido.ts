import type { NombreIcono } from "../../shared/components/Icono";

/* --- Nosotros --------------------------------------------------------------- */

export const SEO_NOSOTROS = {
  titulo: "Nosotros: el equipo que desarrolla el CRM",
  descripcion:
    "Somos estudiantes de Ingeniería en Informática de la UNLaM que desarrollamos Ztech CRM como trabajo práctico, con React, Spring Boot y PostgreSQL.",
};

export const NOSOTROS_HERO = {
  titular: "Un equipo de estudiantes, un producto real",
  bajada:
    "Somos un grupo de estudiantes de Ingeniería en Informática de la Universidad Nacional de La Matanza que construye Ztech CRM como trabajo práctico de la materia Gestión Aplicada al Desarrollo de Software II, ciclo 2026.",
};

export const NOSOTROS_HISTORIA = {
  titulo: "Cómo nació el proyecto",
  parrafos: [
    "Ztech CRM nació en la materia Gestión Aplicada al Desarrollo de Software II de la carrera de Ingeniería en Informática de la UNLaM. El trabajo práctico pedía construir un producto de software completo, de punta a punta, y el equipo eligió enfocarse en un problema concreto: la gestión comercial de salones para eventos corporativos de hasta 50 personas.",
    "Un CRM genérico maneja clientes y ventas en abstracto. Ztech CRM está pensado para un negocio específico: el modelo de datos une cada oportunidad a un salón, con su capacidad y su disponibilidad, y el embudo comercial refleja las etapas reales por las que pasa una reserva de evento corporativo, desde la consulta hasta la confirmación.",
    "El equipo trabaja en entregas incrementales, con cada etapa del desarrollo documentada y revisada antes de avanzar a la siguiente. Las decisiones de diseño y las pruebas del sistema quedan registradas junto con el código, para que el resultado se pueda evaluar y seguir mejorando etapa por etapa, como corresponde a un trabajo académico serio.",
  ],
};

export const NOSOTROS_VALORES: {
  titulo: string;
  items: { icono: NombreIcono; titulo: string; texto: string }[];
} = {
  titulo: "Cómo encaramos el trabajo",
  items: [
    {
      icono: "capas",
      titulo: "Foco específico",
      texto:
        "Preferimos resolver bien un problema acotado (la gestión comercial de salones corporativos) antes que ofrecer una herramienta genérica que no termine de encajar con nadie.",
    },
    {
      icono: "propuesta",
      titulo: "Trabajo documentado",
      texto:
        "Cada decisión de diseño, cada cambio de alcance y cada prueba realizada queda registrada, para que el proyecto se pueda entender sin depender de una sola persona.",
    },
    {
      icono: "tendencia",
      titulo: "Entregas incrementales",
      texto:
        "Avanzamos en etapas cortas y funcionales, en vez de prometer todo el producto terminado de una sola vez, así cada entrega se puede probar y corregir a tiempo.",
    },
    {
      icono: "escudo",
      titulo: "Alcance honesto",
      texto:
        "Este es un proyecto académico en desarrollo, no un producto terminado en el mercado, y lo comunicamos así en cada página en lugar de exagerar lo que hoy hace.",
    },
  ],
};

export const NOSOTROS_STACK = {
  titulo: "La tecnología detrás del CRM",
  bajada:
    "Ztech CRM está construido con tecnología abierta y ampliamente usada en la industria, elegida para que el proyecto sea mantenible, testeable y fácil de entender por cualquier integrante del equipo.",
  items: [
    {
      titulo: "Frontend",
      texto:
        "Interfaz construida con React y TypeScript, pensada para que el tablero del embudo sea rápido de usar.",
    },
    {
      titulo: "Backend",
      texto:
        "Lógica de negocio en Java con Spring Boot, que valida la disponibilidad de cada salón antes de confirmar una reserva.",
    },
    {
      titulo: "Base de datos",
      texto:
        "Toda la información de empresas, contactos, oportunidades y salones se guarda en PostgreSQL, como única fuente de verdad del negocio.",
    },
    {
      titulo: "Metodología",
      texto:
        "El equipo trabaja con entregas incrementales y control de versiones, documentando cada decisión técnica del proyecto.",
    },
  ],
};

/* --- Contacto ---------------------------------------------------------------- */

export const SEO_CONTACTO = {
  titulo: "Contacto: escribinos sobre el proyecto",
  descripcion:
    "Escribinos si querés ver una demo de Ztech CRM, tenés una consulta sobre el proyecto académico o querés contactar al equipo que lo desarrolla.",
};

export const CONTACTO_HERO = {
  titular: "Hablemos sobre tu salón de eventos",
  bajada:
    "Contanos qué necesitás (una demo del CRM, una consulta académica sobre el proyecto o una propuesta) y te respondemos desde el equipo que desarrolla Ztech CRM.",
};

export const MOTIVOS_CONSULTA = [
  "Pedir una demo",
  "Consulta sobre el proyecto",
  "Consulta académica",
  "Propuesta de colaboración",
  "Reporte de error",
  "Otra consulta",
];

export const PREGUNTAS = [
  {
    pregunta: "¿Ztech CRM ya está disponible para usar en producción?",
    respuesta:
      "No todavía. Ztech CRM es un proyecto académico en desarrollo, hecho por estudiantes de Ingeniería en Informática de la UNLaM como trabajo práctico. Vas a poder seguir su evolución, pero hoy no es un producto comercial a la venta.",
  },
  {
    pregunta: "¿Para qué tipo de salones sirve el sistema?",
    respuesta:
      "Está pensado para salones que alojan eventos corporativos de hasta 50 personas: capacitaciones, convenciones, lanzamientos, reuniones de directorio y workshops. No apunta a salones de fiestas ni a eventos masivos.",
  },
  {
    pregunta: "¿Cómo evita el sistema que se sobrevenda una fecha?",
    respuesta:
      "Cada salón tiene su capacidad y su agenda cargadas en el sistema. Al confirmar una oportunidad como ganada, el sistema verifica la disponibilidad del salón para esa fecha y ese horario antes de dejarla cerrada.",
  },
  {
    pregunta: "¿Qué pasa con una oportunidad que se pierde?",
    respuesta:
      "Se marca como perdida y queda registrado el motivo (precio, disponibilidad, elección de otro salón, entre otros). Ese dato queda en el historial de la empresa, para entender por qué se cayó cada negociación.",
  },
  {
    pregunta: "¿Quiénes pueden usar el sistema dentro de un salón?",
    respuesta:
      "El sistema define roles con distintos permisos (por ejemplo, administrador, responsable comercial y vendedor), para que cada persona vea y modifique sólo la información que corresponde a su función dentro del equipo.",
  },
  {
    pregunta: "¿Qué tecnología usa Ztech CRM por dentro?",
    respuesta:
      "El frontend está hecho con React y TypeScript, el backend con Java y Spring Boot, y los datos se guardan en PostgreSQL. Es la misma tecnología que se usa en desarrollos profesionales de sistemas de gestión.",
  },
];
