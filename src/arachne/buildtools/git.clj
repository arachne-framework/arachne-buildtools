(ns arachne.buildtools.git
  "Tools for naming and finding git artifacts by SHA"
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [boot.core :as b]
            [boot.task.built-in :as task]))

(defn throw-if-not-clean
  "Throw an exception if the git project at the given directory is not clean."
  [git-dir msg]
  (let [f (.getAbsoluteFile (io/file git-dir))
        status (:out (sh/sh "git" "status" :dir git-dir))]
    (when-not (re-find #"nothing to commit, working directory clean" status)
      (throw (ex-info msg {:git-dir git-dir, :file f})))))

(defn current-sha
  "Return the SHA of the current HEAD as given by 'git rev-parse
  --short', in the given (process relative) directory.

  It's safe to use the --short version because git will automatically
  spit out more characters if it ever becomes ambiguous, which will
  guarantees the artifact names will always be unique."
  [git-dir]
  (let [f (.getAbsoluteFile (io/file git-dir))
        sha (s/trim (:out (sh/sh "git" "rev-parse" "--short" "HEAD" :dir f)))]
    (when (s/blank? sha)
      (throw (ex-info (format "Could not find git repository at %s "
                              (.getAbsolutePath f))
                      {:dir git-dir
                       :file f
                       :path (.getAbsolutePath f)})))
    sha))

(defn exec!
  "Run a shell command (as per sh/sh), throwing with an error message if it did
  not return zero"
  [& args]
  (let [result (apply sh/sh args)]
    (when-not (zero? (:exit result))
      (println result)
      (throw (ex-info (format "Command '%s' returned a non-zero exit code"
                        (s/join " " args))
               {:result result})))
    result))

;; TODO: Boot could be optimized to run inline in the same JVM instead of
;; shelling out to a boot subprocess, but can't figure out how to do it at the
;; moment.

(defn ensure-installed!
  "Given a git dep form, clone the repository, checkout the ref, and install.
  Return the actual SHA that was installed. Throw if anything went wrong."
  [[artifact [repo ref]]]
  (let [clone-dir-file (io/file (str "./.git-deps/") (str artifact))
        clone-dir (.getCanonicalPath clone-dir-file)]
    (if (.exists clone-dir-file)
      (exec! "git" "fetch" repo :dir clone-dir)
      (exec! "git" "clone" repo clone-dir))
    (exec! "git" "checkout" ref :dir clone-dir)
    (let [result (exec! "boot" "build" :dir clone-dir)]
      (second (re-find #"Installed Version:\s*(.*)$" (:out result))))))
