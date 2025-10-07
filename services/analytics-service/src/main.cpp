#include <iostream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <mutex>
#include <httplib.h>
#include <nlohmann/json.hpp>
#include <prometheus/counter.h>
#include <prometheus/exposer.h>
#include <prometheus/registry.h>
#include <prometheus/histogram.h>

using json = nlohmann::json;
using namespace prometheus;

class AnalyticsService {
private:
    std::shared_ptr<Registry> registry;
    Family<Counter> &request_counter;
    Family<Histogram> &latency_histogram;

    struct OrderMetrics {
        std::atomic<uint64_t> total_orders{0};
        std::atomic<uint64_t> successful_orders{0};
        std::atomic<uint64_t> failed_orders{0};
        double total_revenue{0.0};  // Not atomic, use mutex instead
        mutable std::mutex revenue_mutex;
    };

    OrderMetrics metrics;

public:
    AnalyticsService() 
        : registry([]() { return std::make_shared<Registry>(); }()),  // Lambda workaround
          request_counter(BuildCounter()
                .Name("analytics_requests_total")
                .Help("Total number of analytics requests")
                .Register(*registry)),
          latency_histogram(BuildHistogram()
                .Name("analytics_request_duration_seconds")
                .Help("Analytics request duration")
                .Register(*registry)) {}

    void processOrderEvent(const json &order_data) {
        auto start = std::chrono::high_resolution_clock::now();

        try {
            std::string status = order_data["status"];

            metrics.total_orders++;

            if (status == "SUCCESS" || status == "COMPLETED") {
                metrics.successful_orders++;

                if (order_data.contains("amount")) {
                    double amount = order_data["amount"];
                    std::lock_guard<std::mutex> lock(metrics.revenue_mutex);
                    metrics.total_revenue += amount;
                }

            } else if (status == "FAILED") {
                metrics.failed_orders++;
            }

            request_counter.Add({{"status", "success"}}).Increment();

        } catch (const std::exception &e) {
            std::cerr << "Error processing order: " << e.what() << std::endl;
            request_counter.Add({{"status", "error"}}).Increment();
        }

        auto end = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration<double>(end - start).count();
        latency_histogram.Add({}, Histogram::BucketBoundaries{
            .005, .01, .025, .05, .075, .1, .25, .5, .75, 1.0, 2.5, 5.0, 7.5, 10.0
        }).Observe(duration);
    }

    json getAnalysis() const {
        double revenue;
        {
            std::lock_guard<std::mutex> lock(metrics.revenue_mutex);
            revenue = metrics.total_revenue;
        }
        
        return {
            {"total_orders", metrics.total_orders.load()},
            {"successful_orders", metrics.successful_orders.load()},
            {"failed_orders", metrics.failed_orders.load()},
            {"success_rate", calculateSuccessRate()},
            {"total_revenue", revenue},
            {"timestamp", std::chrono::system_clock::now().time_since_epoch().count()}
        };
    }

    json getAggregatedMetrics(const std::string &time_window) const {
        return {
            {"time_window", time_window},
            {"metrics", getAnalysis()},
            {"throughput", calculateThroughput()},
            {"avg_order_value", calculateAvgOrderValue()}
        };
    }

    std::shared_ptr<Registry> getRegistry() {
        return registry;
    }

private:
    double calculateSuccessRate() const {
        uint64_t total = metrics.total_orders.load();
        if (total == 0) return 0.0;
        return (static_cast<double>(metrics.successful_orders.load()) / total) * 100.0;
    }

    double calculateThroughput() const {
        return metrics.total_orders.load() / 60.0;
    }

    double calculateAvgOrderValue() const {
        uint64_t successful = metrics.successful_orders.load();
        if (successful == 0) return 0.0;
        
        std::lock_guard<std::mutex> lock(metrics.revenue_mutex);
        return metrics.total_revenue / successful;
    }
};

int main() {
    // todo: implementa main.

    std::cout << "starting Analytics Service..." << std::endl;

    AnalyticsService analytics;

    Exposer exposer{"0.0.0.0.8083"};
    exposer.RegisterCollectable(analytics.getRegistry());

    //http server for rest api
    httplib::Server server;

    //health
    server.Get("/health", [](const httplib::Request&, httplib::Response &res)  {
        res.set_content("{\"status\":\"UP\"}", "application/json");
    });

    //get analytics endpoint
    server.Get("/api/analytics", [&analytics](const httplib::Request&, httplib::Response& res) {
        json response = analytics.getAnalysis();
        res.set_content(response.dump(), "application/json");
    });

    //get aggregated metrics endpoint
    server.Get("/api/analytics/aggregated", [&analytics](const httplib::Request& req, httplib::Response& res) {
        std::string time_window = req.has_param("window") ? req.get_param_value("window") : "1h";
        json response = analytics.getAggregatedMetrics(time_window);
        res.set_content(response.dump(), "application/json");
    });

    //process order event endpoint
    server.Post("api/analytics/events/order", [&analytics](const httplib::Request& req, httplib::Response& res) {
        try {
            json order_data = json::parse(req.body);
            analytics.processOrderEvent(order_data);
            res.status = 202;
            res.set_content("{\"status\":\"accepted\"}", "application/json");

        } catch (const std::exception& e) {
            res.status = 400;
            json error = {{"error", e.what()}};
            res.set_content(error.dump(), "application/json");
        }
    });

    //metrics endpoint
    server.Get("/metrics", [](const httplib::Request&, httplib::Response& res) {
        res.set_content("# prometheus metrics at :8083/metrics\n", "test/plain");
    });

    std::cout << "analytics service running on port 8084" << std::endl;
    std::cout << "prometheus metrics on port 8083" << std::endl;
    std::cout << "health check at http://localhost:8084/health" << std::endl;

    server.listen("0.0.0.0", 8084);

    return 0;
}