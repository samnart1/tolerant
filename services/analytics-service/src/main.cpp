#include <iostream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
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
        std::atomic<uint64_t> failed_orderes{0};
        std::atomic<double> total_revenue{0.0};
    };

    OrderMetrics metrics;

public:
    AnalyticsService() 
        : registry(std::make_shared<Registry()),
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
            std::string status = order_data["status"]

            metrics.total_orders++;

            if (status == "SUCCESS" || status == "COMPLETED") {
                metrics.successful_orders++;

                if (order_data.contains("amount")) {
                    double amount = order_data["amount"];
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
        return {
            {"total_orders", metrics.total_orders.load()},
            {"successfull_orders", metrics.successful_orders.load()},
            {"failed_orders", metrics.failed_orders.load()},
            {"success_rate", calculateSuccessRate()},
            {"total_revenue", metrics.total_revenue.load()},
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

private:
    double calculateSuccessRate() const {
        uint64_t total = metrics.total_orders.load();
        if (total == 0) return 0.0;
        return (static_cast<double>(metrics.successful_orders.load())/total)*100.0
    }

    double calculateThroughput() const {
        //would need actual time tracking or something
        return metrics.total_orders.load() /60.0; //per minute
    }

    double calculateAvgOrderValue() const {
        uint64_t successful = metrics.successful_orders.load();
        if (successful == 0) return 0.0;
        return metrics.total_revenue.load() / successful;
    }

    std::shared_ptr<Registry> getRegistry() {
        return registry;
    }

    // friend int main();

};

// int main() {}