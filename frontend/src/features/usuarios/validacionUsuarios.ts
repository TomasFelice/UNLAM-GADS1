import { z } from "zod";

export const esquemaClaveTemporal = z.string().regex(
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,72}$/,
  "Usá entre 10 y 72 caracteres, con mayúscula, minúscula, número y símbolo.",
);
