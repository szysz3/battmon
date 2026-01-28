#!/usr/bin/env python3
import os
import subprocess
import logging
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('apcaccess-proxy')

# Configuration
HOST = os.getenv('APCACCESS_PROXY_HOST', '0.0.0.0')
PORT = int(os.getenv('APCACCESS_PROXY_PORT', '8083'))
APCACCESS_COMMAND = ['apcaccess', 'status']


class ApcAccessHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        logger.info("%s - %s" % (self.address_string(), format % args))

    def do_GET(self):
        parsed_path = urlparse(self.path)

        if parsed_path.path == '/apcaccess':
            self.handle_apcaccess()
        elif parsed_path.path == '/health':
            self.handle_health()
        else:
            self.send_error(404, "Endpoint not found")

    def handle_apcaccess(self):
        try:
            parsed_path = urlparse(self.path)
            query = parse_qs(parsed_path.query)
            host = query.get('host', [None])[0]
            port = query.get('port', [None])[0]

            if host:
                port_value = port or '3551'
                command = ['apcaccess', '-h', f'{host}:{port_value}', 'status']
            else:
                command = APCACCESS_COMMAND

            logger.debug(f"Executing command: {' '.join(command)}")
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                timeout=10
            )

            if result.returncode != 0:
                logger.error(f"apcaccess failed with exit code {result.returncode}: {result.stderr}")
                self.send_error(500, f"apcaccess failed: {result.stderr}")
                return

            output = result.stdout
            logger.debug(f"apcaccess output length: {len(output)} bytes")

            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; charset=utf-8')
            self.send_header('Content-Length', str(len(output.encode('utf-8'))))
            self.end_headers()
            self.wfile.write(output.encode('utf-8'))

        except subprocess.TimeoutExpired:
            logger.error("apcaccess command timed out")
            self.send_error(504, "apcaccess command timed out")
        except FileNotFoundError:
            logger.error("apcaccess command not found - is apcupsd installed?")
            self.send_error(500, "apcaccess command not found")
        except Exception as e:
            logger.exception("Unexpected error executing apcaccess")
            self.send_error(500, f"Internal error: {str(e)}")

    def handle_health(self):
        self.send_response(200)
        self.send_header('Content-Type', 'text/plain')
        self.end_headers()
        self.wfile.write(b'OK')


def main():
    server_address = (HOST, PORT)
    httpd = HTTPServer(server_address, ApcAccessHandler)

    logger.info(f"Starting apcaccess proxy server on {HOST}:{PORT}")
    logger.info(f"Command: {' '.join(APCACCESS_COMMAND)}")
    logger.info("Endpoints:")
    logger.info(f"  - http://{HOST}:{PORT}/apcaccess - Execute apcaccess and return output")
    logger.info(f"  - http://{HOST}:{PORT}/health - Health check")

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down server...")
        httpd.shutdown()


if __name__ == '__main__':
    main()
