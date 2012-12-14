using CommandLine;
using clojure.lang.Hosting;
using System.Windows.Forms;

namespace Console
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
            var cmdParser = new CommandLineParser();
            cmdParser.ParseArguments(args, options);

            Clojure.AddNamespaceDirectoryMapping("clojurewm", @"src\clojurewm");

            var replInit = "(use 'clojurewm.init)";
            replInit += "(in-ns 'clojurewm.init)";

            if (options.ClojureRepl)
                Clojure.Require("clojure.main").main("-e", replInit, "-r");
        }
    }
}
