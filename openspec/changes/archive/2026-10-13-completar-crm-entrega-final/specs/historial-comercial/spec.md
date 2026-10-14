# Spec Delta

## Purpose

Registrar hechos comerciales y reconstruir el recorrido de clientes y negociaciones con autoría, cronología y permisos consistentes.

## ADDED Requirements

### Requirement: Registro de actividades realizadas
El sistema SHALL permitir registrar una actividad con tipo activo, fecha/hora real no futura, descripción, resultado y al menos una empresa, contacto u oportunidad autorizada. SHALL fijar actor, tenant y fecha de registro desde el servidor y rechazar asociaciones contradictorias. Las actividades SHALL ser append-only, sin edición ni borrado por API.

#### Scenario: Actividad desde oportunidad
- **WHEN** se registra una llamada realizada en una oportunidad propia
- **THEN** se derivan empresa y contacto desde la oportunidad y la actividad aparece en los tres históricos aplicables

#### Scenario: Actividad desde contacto
- **WHEN** se registra una interacción de un contacto perteneciente a una empresa
- **THEN** se deriva la empresa del contacto; un contacto individual puede registrar la actividad sin empresa

#### Scenario: Fecha futura o asociaciones contradictorias
- **WHEN** se registra una actividad futura o se envían empresa/contacto diferentes de los de la oportunidad
- **THEN** se rechaza sin crear el hecho histórico ni aceptar un actor enviado por el request

### Requirement: Consulta cronológica completa
El sistema SHALL mostrar actividades por empresa, contacto y oportunidad en orden descendente estable de fecha real y permitir recorrer todas las páginas. SHALL distinguir fecha del hecho y del registro, identificar autor, tipo, descripción y resultado y resolver referencias inactivas históricas.

#### Scenario: Más de una página y mismo instante
- **WHEN** existen más de 20 actividades y varias comparten fecha real
- **THEN** la navegación con orden estable permite recuperar todas sin truncamiento ni duplicaciones por empates

#### Scenario: Autor desactivado
- **WHEN** se desactiva al usuario que registró una actividad
- **THEN** el hecho conserva su autoría y sigue visible a los usuarios autorizados

### Requirement: Historial completo de etapas y cierres
El sistema SHALL conservar etapa inicial, cada transición, cierre y reapertura como registros independientes con etapa anterior/destino, fecha, usuario y nota aplicable. SHALL permitir reconstruir cuándo y cómo terminó cada ciclo, incluidos valor final o motivo de pérdida, aun después de una reapertura, sin ofrecer edición o eliminación del historial.

#### Scenario: Ganar, reabrir y perder
- **WHEN** una oportunidad pasa por cierre ganado, reapertura y cierre perdido
- **THEN** se consultan ambos cierres y sus datos, la nota de reapertura y todos los cambios anteriores en orden estable

### Requirement: Visibilidad comercial del historial
El sistema SHALL aplicar tenant y alcance a consultas y altas de actividades. Un vendedor SHALL registrar en clientes asignados directamente o en oportunidades propias; la lectura de un cliente derivada de una oportunidad propia no otorga escritura general. Una actividad de oportunidad ajena no SHALL hacerse visible por compartir empresa/contacto.

#### Scenario: Cliente compartido entre vendedores
- **WHEN** un vendedor consulta la historia de una empresa visible con oportunidades de varios responsables
- **THEN** no recibe actividades vinculadas a oportunidades ajenas ni datos de otro tenant

#### Scenario: Escritura fuera de alcance
- **WHEN** se intenta registrar una actividad en una oportunidad ajena o un cliente sólo visible por relación indirecta sin oportunidad propia indicada
- **THEN** se devuelve `404` sin registrar la actividad
