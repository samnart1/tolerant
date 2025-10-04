-- WRK Lua script for baseline load testing
-- Usage: wrk -t4 -c100 -d60s -s baseline-test.lua http://order-service:8080

local counter = 1
local threads = {}

function setup(thread)
   thread:set("id", counter)
   table.insert(threads, thread)
   counter = counter + 1
end

function init(args)
   requests  = 0
   responses = 0
   
   local msg = "thread %d created"
   print(msg:format(id))
end

-- Product IDs to cycle through
local products = {
   "PROD-001",
   "PROD-002",
   "PROD-003",
   "PROD-004",
   "PROD-005"
}

local payment_methods = {
   "credit_card",
   "debit_card",
   "paypal"
}

function request()
   requests = requests + 1
   
   -- Cycle through products
   local product = products[(requests % #products) + 1]
   local payment = payment_methods[(requests % #payment_methods) + 1]
   local quantity = math.random(1, 5)
   
   local body = string.format([[{
      "productId": "%s",
      "quantity": %d,
      "paymentMethod": "%s"
   }]], product, quantity, payment)
   
   local headers = {}
   headers["Content-Type"] = "application/json"
   
   return wrk.format("POST", "/api/v1/orders", headers, body)
end

function response(status, headers, body)
   responses = responses + 1
   
   if status ~= 201 and status ~= 200 then
      print("Error: status " .. status)
      print("Body: " .. body)
   end
end

function done(summary, latency, requests)
   io.write("------------------------------\n")
   io.write("Test Summary\n")
   io.write("------------------------------\n")
   io.write(string.format("Total Requests: %d\n", summary.requests))
   io.write(string.format("Total Errors: %d\n", summary.errors.status + summary.errors.read + summary.errors.write + summary.errors.timeout))
   io.write(string.format("Requests/sec: %.2f\n", summary.requests / summary.duration * 1000000))
   io.write(string.format("Avg Latency: %.2fms\n", latency.mean / 1000))
   io.write(string.format("50th Percentile: %.2fms\n", latency:percentile(50) / 1000))
   io.write(string.format("95th Percentile: %.2fms\n", latency:percentile(95) / 1000))
   io.write(string.format("99th Percentile: %.2fms\n", latency:percentile(99) / 1000))
   io.write(string.format("Max Latency: %.2fms\n", latency.max / 1000))
end