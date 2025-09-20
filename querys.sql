SELECT * 
FROM view_pedidos_pagados
WHERE DATE(fecha) = '2025-08-10';

SELECT * 
FROM view_pedidos_cancelados
WHERE DATE(fecha) = '2025-08-10';