(ns scraper-engine.core
  (:require [cheshire.core :as c]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn make-recommendation-request [cid offset]
  (let [url    "https://shopee.sg/api/v4/recommend/recommend"
        params {:bundle    "category_landing_page"
                :cat_level 1
                :catid     cid
                :limit     60
                :offset    offset}]
    (map-indexed (fn [ind item]
                   (assoc (update
                           (select-keys item [:itemid :name :price])
                           :name (fn [x]
                                   (str/replace x "," "")))
                          :order-in-page (inc ind)
                          :page-number offset))
                 (->  (:body (http/get url {:query-params params}))
                      (c/parse-string  true)
                      (get-in [:data :sections])
                      first
                      (get-in [:data :item])))))

(defn read-file [file-path]
  (with-open [rdr (io/reader file-path)]
    (doall (line-seq rdr))))

(defn scrap-page-data [url]
  (println "-------Processing URL --------" url)
  (let [cid (-> (str/split url #"\-cat.") last)]
    (mapcat #(make-recommendation-request cid %) (range 3))))

(defn write-to-csv [output-file json-data]
  (let [header (->>  json-data
                     first
                     keys
                     (map #(name %))
                     (str/join ","))
        rows (->> json-data
                  (map (fn [data] (->>
                                   (-> data
                                       (select-keys [:itemid :name :price :order-in-page :page-number])
                                       (vals))
                                   (interpose ",")
                                   (apply str))))
                  (str/join "\n"))
        csv-content (str (str/join "" [header "\n" rows]))]
    (with-open [wrt (io/writer output-file)]
      (.write wrt csv-content))))

(defn -main []
  (->> (read-file "resources/categoryUrls.txt")
       (mapcat scrap-page-data)
       (write-to-csv "resources/output.csv")))

(comment


  (scrap-page-data "https://shopee.sg/Food-Beverages-cat.11011871")

  (count  (scrap-page-data "https://shopee.sg/Food-Beverages-cat.11011871"))

  (scrap-page-data "https://shopee.sg/Toys-Kids-Babies-cat.11011538")

  (count   (scrap-page-data "https://shopee.sg/Toys-Kids-Babies-cat.11011538"))

  (scrap-page-data "https://shopee.sg/Pet-Food-Supplies-cat.11012453 ")


  (-main)


  (count (-main))


  (->> (read-file "resources/categoryUrls.txt")
       (mapcat scrap-page-data)
       (map (fn [data] (apply str (interpose "," (-> data
                                                     (select-keys [:itemid :name :price :order-in-page :page-number])
                                                     (vals)))))))


  (->> (read-file "resources/categoryUrls.txt")
       (mapcat scrap-page-data)
       (mapcat (fn [data] (-> data
                              (select-keys [:itemid :name :price :order-in-page :page-number])
                              (vals)))))

  (->> (read-file "resources/categoryUrls.txt")
       (mapcat scrap-page-data)
       first
       keys
       (map #(name %))
       (str/join ","))

  (-main))