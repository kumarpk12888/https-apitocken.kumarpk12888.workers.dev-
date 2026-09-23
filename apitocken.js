export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: cors });
    }

    // API authentication
    const auth = request.headers.get("Authorization") || "";

    if (!env.API_TOKEN || auth !== `Bearer ${env.API_TOKEN}`) {
      return new Response(
        JSON.stringify({
          ok: false,
          error: "Unauthorized"
        }),
        {
          status: 401,
          headers: {
            ...cors,
            "Content-Type": "application/json"
          }
        }
      );
    }

    const url = new URL(request.url);

    // Health check
    if (request.method === "GET" && url.pathname === "/health") {
      return new Response(
        JSON.stringify({
          ok: true,
          service: "Hard Security Guard",
          status: "online"
        }),
        {
          headers: {
            ...cors,
            "Content-Type": "application/json"
          }
        }
      );
    }

    // Security report
    if (request.method === "POST" && url.pathname === "/report") {
      const body = await request.text();

      if (body.length > 100000) {
        return new Response(
          JSON.stringify({
            ok: false,
            error: "Report too large"
          }),
          {
            status: 413,
            headers: {
              ...cors,
              "Content-Type": "application/json"
            }
          }
        );
      }

      let report;

      try {
        report = JSON.parse(body);
      } catch {
        return new Response(
          JSON.stringify({
            ok: false,
            error: "Invalid JSON"
          }),
          {
            status: 400,
            headers: {
              ...cors,
              "Content-Type": "application/json"
            }
          }
        );
      }

      return new Response(
        JSON.stringify({
          ok: true,
          received: true,
          message: "Security report received",
          score: report?.score ?? null
        }),
        {
          headers: {
            ...cors,
            "Content-Type": "application/json"
          }
        }
      );
    }

    return new Response(
      JSON.stringify({
        ok: false,
        error: "Not Found"
      }),
      {
        status: 404,
        headers: {
          ...cors,
          "Content-Type": "application/json"
        }
      }
    );
  }
};
