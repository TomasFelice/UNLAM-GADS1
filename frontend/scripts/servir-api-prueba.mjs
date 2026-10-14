import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { delimiter, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const backend = fileURLToPath(new URL("../../backend/", import.meta.url));
const dependencias = (await readFile(resolve(backend, "target/baseline-classpath.txt"), "utf8")).trim();
const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, "bin/java.exe") : "java";
const api = spawn(java, ["-cp", [resolve(backend, "target/test-classes"), resolve(backend, "target/classes"), dependencias].join(delimiter),
  "com.ztech.crm.support.LoadBaselineApplication"], { cwd: backend, windowsHide: true, stdio: ["pipe", "inherit", "inherit"] });
const cerrar = () => api.stdin.end("stop\n");
process.on("SIGINT", cerrar);
process.on("SIGTERM", cerrar);
api.on("exit", codigo => process.exit(codigo ?? 1));
api.on("error", error => { console.error(error.message); process.exitCode = 1; });
