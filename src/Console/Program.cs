using System.Reflection;
using System.Threading;
using CommandLine;
using clojure.lang.Hosting;
using System.Windows.Forms;
using clojurewm;

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
            //Clojure.AddNamespaceDirectoryMapping("clojure", @"ClojureClrEx\src\clojure");

            var replInit = "(use 'clojurewm.init)";
            replInit += "(in-ns 'clojurewm.init)";

            var native = new Native();

            //var foo = new globalKeyboardHook();

            // required for message hooks.
            //Application.Run();

            if (options.ClojureRepl)
                Clojure.Require("clojure.main").main("-e", replInit, "-r");
            else
            {
               ThreadPool.QueueUserWorkItem(state => native.Init());
            }

            while (true)
            {
                
            }

            // required for message hooks.
        }
    }
}
