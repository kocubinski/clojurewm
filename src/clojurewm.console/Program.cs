using System.Reflection;
using CommandLine;
using clojure.lang.Hosting;

namespace clojurewm.console
{
    class Program
    {
        class Options
        {
            [Option("c", "clj", DefaultValue = false)]
            public bool ClojureRepl { get; set; }
        } 

        static private readonly Options options = new Options();

        static void Main(string[] args)
        {
            Assembly.Load("clojurewm");
            var cmdParser = new CommandLineParser();
            cmdParser.ParseArguments(args, options);

            Clojure.AddNamespaceDirectoryMapping("clojurewm", @"src\clojurewm");

            var replInit = "(use 'clojurewm.init)\n" + 
              "(in-ns 'clojurewm.init)\n" +
              "(main)";

            if (options.ClojureRepl)
                Clojure.Require("clojure.main").main("-e", replInit, "-r");
        }
    }
}
